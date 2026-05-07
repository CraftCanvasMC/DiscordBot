package io.canvasmc.bot.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DownloadService {
    private static final String SITE_BASE = "https://canvasmc.io";
    private static final String API_BASE = "https://canvasmc.io/api/v2";

    private static final DownloadService INSTANCE = new DownloadService();

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private DownloadService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = JacksonResources.create().getObjectMapper();
    }

    public static DownloadService getInstance() {
        return INSTANCE;
    }

    public List<BuildInfo> getBuilds(String project, String channel) throws IOException, InterruptedException {
        String normalizedProject = normalizeProject(project);
        StringBuilder url = new StringBuilder(API_BASE)
                .append("/builds?project=")
                .append(URLEncoder.encode(normalizedProject, StandardCharsets.UTF_8));

        if (channel != null && !channel.isBlank()) {
            url.append("&channel=")
                    .append(URLEncoder.encode(channel.trim(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "CanvasMCBot")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download API returned status " + response.statusCode());
        }

        BuildsResponse payload = mapper.readValue(response.body(), BuildsResponse.class);
        if (payload == null || payload.builds == null) {
            return List.of();
        }

        return payload.builds.stream()
                .filter(BuildInfo::isSuccessful)
                .filter(build -> build.downloadUrl != null && !build.downloadUrl.isBlank())
                .sorted(Comparator.comparingInt(BuildInfo::buildNumber).reversed())
                .toList();
    }

    public BuildInfo getLatestBuild(String project) throws IOException, InterruptedException {
        List<BuildInfo> builds = getBuilds(project, null);
        if (builds.isEmpty()) {
            throw new IOException("No successful downloadable builds found");
        }
        return builds.get(0);
    }

    public static String downloadsPage(String project) {
        if (project == null || project.isBlank()) {
            return SITE_BASE + "/downloads";
        }
        return SITE_BASE + "/downloads/" + normalizeProject(project);
    }

    public static String latestPage(String project) {
        return SITE_BASE + "/downloads/latest?project=" + URLEncoder.encode(normalizeProject(project), StandardCharsets.UTF_8);
    }

    public static String trackedDownloadUrl(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            return downloadsPage(null);
        }
        return API_BASE + "/download?url=" + URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8);
    }

    public static String normalizeProject(String project) {
        String value = project == null ? "" : project.trim().toLowerCase(Locale.ROOT);
        if (value.equals("canvas") || value.equals("horizon")) {
            return value;
        }
        return "canvas";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BuildsResponse {
        @JsonProperty("builds")
        List<BuildInfo> builds;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuildInfo {
        @JsonProperty("result")
        private String result;

        @JsonProperty("buildNumber")
        private int buildNumber;

        @JsonProperty("channelVersion")
        private String channelVersion;

        @JsonProperty("downloadUrl")
        private String downloadUrl;

        @JsonProperty("commits")
        private List<Commit> commits;

        public int buildNumber() {
            return buildNumber;
        }

        public boolean isSuccessful() {
            return "SUCCESS".equalsIgnoreCase(result);
        }

        public String channelVersion() {
            return channelVersion;
        }

        public String downloadUrl() {
            return downloadUrl;
        }

        public String trackedDownloadUrl() {
            return DownloadService.trackedDownloadUrl(downloadUrl);
        }

        public List<Commit> commits() {
            return commits;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Commit {
            @JsonProperty("message")
            public String message;

            @JsonProperty("author")
            public String author;

            @JsonProperty("hash")
            public String hash;

            @JsonProperty("extraDescription")
            public String extraDescription;
        }
    }
}
