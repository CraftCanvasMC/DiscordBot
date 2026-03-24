package io.canvasmc.bot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocsSearchService {
    private static final Logger log = LoggerFactory.getLogger(DocsSearchService.class);
    private static final String DOCS_OWNER = "CraftCanvasMC";
    private static final String DOCS_REPO = "Docs";
    private static final String DOCS_BRANCH = "main";
    private static final String DOCS_PREFIX = "src/content/docs/";
    private static final String DOCS_SITE = "https://docs.canvasmc.io";
    private static final Duration INDEX_TTL = Duration.ofHours(6);

    private static final DocsSearchService INSTANCE = new DocsSearchService();

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final ExecutorService refreshExecutor;

    private volatile List<DocPage> pages = List.of();
    private volatile Instant lastRefresh = Instant.EPOCH;
    private volatile boolean refreshInProgress = false;

    private DocsSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = JacksonResources.create().getObjectMapper();
        this.refreshExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "docs-search-refresh");
            t.setDaemon(true);
            return t;
        });
        triggerRefreshIfNeeded(true);
    }

    public static DocsSearchService getInstance() {
        return INSTANCE;
    }

    public SearchResult search(String project, String keyword, int limit) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        if (cleanKeyword.length() <= 2) {
            return SearchResult.error("Please use a keyword with 2 or more characters.");
        }

        triggerRefreshIfNeeded(false);
        List<DocPage> currentPages = pages;
        if (currentPages.isEmpty()) {
            return SearchResult.indexing();
        }

        String scope = normalizeProject(project);
        String needle = cleanKeyword.toLowerCase(Locale.ROOT);

        List<ScoredResult> scored = new ArrayList<>();
        for (DocPage page : currentPages) {
            if (!matchesScope(page.url(), scope)) {
                continue;
            }

            int score = score(page, needle);
            if (score > 0) {
                scored.add(new ScoredResult(page, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredResult::score).reversed()
                .thenComparing(sr -> sr.page().title()));

        List<DocHit> hits = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < limit; i++) {
            hits.add(new DocHit(scored.get(i).page().title(), scored.get(i).page().url()));
        }

        return SearchResult.success(hits, currentPages.size());
    }

    private String normalizeProject(String project) {
        String value = project == null ? "" : project.trim().toLowerCase(Locale.ROOT);
        if (value.equals("canvas") || value.equals("horizon")) {
            return value;
        }
        return "all";
    }

    private boolean matchesScope(String url, String scope) {
        if (scope.equals("all")) {
            return true;
        }
        return url.contains("/" + scope + "/");
    }

    private int score(DocPage page, String needle) {
        int score = 0;
        String title = page.title().toLowerCase(Locale.ROOT);
        String path = page.path().toLowerCase(Locale.ROOT);
        String content = page.searchableText();

        if (title.equals(needle)) score += 10;
        if (title.contains(needle)) score += 6;
        if (path.contains(needle)) score += 4;

        int bodyHits = countOccurrences(content, needle);
        score += Math.min(bodyHits, 12);

        return score;
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = text.indexOf(needle, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + needle.length();
        }
    }

    private void triggerRefreshIfNeeded(boolean force) {
        if (refreshInProgress) {
            return;
        }
        if (!force && !pages.isEmpty() && Duration.between(lastRefresh, Instant.now()).compareTo(INDEX_TTL) < 0) {
            return;
        }

        refreshInProgress = true;
        CompletableFuture.runAsync(this::refreshIndex, refreshExecutor)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        log.warn("Docs index refresh failed", throwable);
                    }
                    refreshInProgress = false;
                });
    }

    private void refreshIndex() {
        try {
            List<String> paths = fetchDocPaths();
            List<DocPage> fetchedPages = new ArrayList<>();
            for (String path : paths) {
                String content = fetchRawFile(path);
                if (content == null || content.isBlank()) {
                    continue;
                }
                DocPage page = toDocPage(path, content);
                if (page != null) {
                    fetchedPages.add(page);
                }
            }

            if (!fetchedPages.isEmpty()) {
                pages = List.copyOf(fetchedPages);
                lastRefresh = Instant.now();
                log.info("Indexed {} documentation pages", fetchedPages.size());
            }
        } catch (Exception e) {
            log.warn("Failed to refresh docs index", e);
        }
    }

    private List<String> fetchDocPaths() throws IOException, InterruptedException {
        String treeUrl = "https://api.github.com/repos/" + DOCS_OWNER + "/" + DOCS_REPO +
                "/git/trees/" + DOCS_BRANCH + "?recursive=1";
        JsonNode root = getJson(treeUrl);
        JsonNode tree = root.get("tree");
        if (tree == null || !tree.isArray()) {
            return List.of();
        }

        List<String> files = new ArrayList<>();
        for (JsonNode entry : tree) {
            if (!"blob".equals(entry.path("type").asText())) {
                continue;
            }
            String path = entry.path("path").asText();
            if (!path.startsWith(DOCS_PREFIX)) {
                continue;
            }
            if (!(path.endsWith(".md") || path.endsWith(".mdx"))) {
                continue;
            }
            files.add(path);
        }
        return files;
    }

    private String fetchRawFile(String path) {
        String encodedPath = encodePath(path);
        String rawUrl = "https://raw.githubusercontent.com/" + DOCS_OWNER + "/" + DOCS_REPO +
                "/" + DOCS_BRANCH + "/" + encodedPath;
        HttpRequest request = HttpRequest.newBuilder(URI.create(rawUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "CanvasMCBot")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            return null;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private DocPage toDocPage(String path, String content) {
        String relative = path.substring(DOCS_PREFIX.length());
        if (relative.endsWith(".mdx")) {
            relative = relative.substring(0, relative.length() - 4);
        } else if (relative.endsWith(".md")) {
            relative = relative.substring(0, relative.length() - 3);
        }

        String route;
        if (relative.equals("index")) {
            route = "/";
        } else if (relative.endsWith("/index")) {
            route = "/" + relative.substring(0, relative.length() - "/index".length()) + "/";
        } else {
            route = "/" + relative + "/";
        }

        String title = extractTitle(content);
        if (title == null || title.isBlank()) {
            title = humanize(relative);
        }

        String searchable = normalizeContent(content);
        return new DocPage(path, title, DOCS_SITE + route, searchable);
    }

    private String extractTitle(String content) {
        Matcher matcher = Pattern.compile("(?m)^title:\\s*(.+)$").matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1).trim();
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private String humanize(String relative) {
        String name = relative;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.replace('-', ' ');
    }

    private String normalizeContent(String content) {
        String text = content.toLowerCase(Locale.ROOT);
        text = text.replaceAll("(?s)^---.*?---", " ");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("[`*_>#\\[\\]{}()|]", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CanvasMCBot")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub API exception: " + response.statusCode() + " for " + url);
        }
        return mapper.readTree(response.body());
    }

    private String encodePath(String path) {
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public record DocHit(String title, String url) {}

    public record SearchResult(State state, String message, List<DocHit> hits, int indexedPages) {
        public static SearchResult success(List<DocHit> hits, int indexedPages) {
            return new SearchResult(State.SUCCESS, "", hits, indexedPages);
        }

        public static SearchResult indexing() {
            return new SearchResult(State.INDEXING, "Please try again later, indexing atm.", List.of(), 0);
        }

        public static SearchResult error(String message) {
            return new SearchResult(State.ERROR, Objects.requireNonNull(message), List.of(), 0);
        }
    }

    private record DocPage(String path, String title, String url, String searchableText) {}

    private record ScoredResult(DocPage page, int score) {}

    public enum State {
        SUCCESS,
        INDEXING,
        ERROR
    }
}