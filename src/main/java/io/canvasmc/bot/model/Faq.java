package io.canvasmc.bot.model;

import discord4j.core.spec.EmbedCreateSpec;
import io.canvasmc.bot.util.Embeds;

public enum Faq {
    FOLIASPREAD("Folia Spread",
            "Canvas will run best when players are spread out. Make sure your server's game mode encourages this behavior!"),

    SCHEDULER("Canvas Schedulers", """
            Canvas supports multiple region schedulers, each with different trade-offs:

            **[EDF](https://docs.canvasmc.io/canvas/scheduler/edf/)** - Folia's default scheduler; most stable and reliable, but least performant.
            **[Work Stealing](https://docs.canvasmc.io/canvas/scheduler/work-stealing/)** - Steals overdue tasks between threads with NUMA-aware scheduling on Linux. Disclaimer: ATP work stealing has task loss issues.
            **[Affinity](https://docs.canvasmc.io/canvas/scheduler/affinity/)** - Canvas's recommended scheduler; significantly improves MSPT/TPS via thread affinity and work stealing.
            **[Beta](https://docs.canvasmc.io/canvas/scheduler/beta-scheduler/)** - Deprecated experimental scheduler by SpottedLeaf; unstable and not recommended for use."""),

    SPARKREPORT("Spark Profiler Report Needed", """
            To help us diagnose your performance issue, please provide a **Spark profiler report**.
            
            **How to generate one:**
            1. Install the [Spark](https://spark.lucko.me/) plugin on your server
            2. Run `/spark profiler start` in your server console
            3. Let it run for **at least 2 minutes** during the issue
            4. Run `/spark profiler stop` to finish
            5. Share the generated link here
            
            > This helps us identify exactly what's causing lag or performance issues."""),

    LOGS("Server Logs Needed", """
            To help us troubleshoot your issue, please share your **server logs**.
            
            **Where to find them:**
            Your latest log file is at `logs/latest.log` in your server directory.
            
            **How to share:**
            1. Upload your `latest.log` file to [mclo.gs](https://mclo.gs/) or [paste.gg](https://paste.gg/)
            2. Share the generated link here
            
            > **Do NOT** paste raw logs directly in chat - always use a paste service!""");

    private final String title;
    private final String description;

    Faq(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() { return title; }
    public String description() { return description; }

    public EmbedCreateSpec toEmbed() {
        return Embeds.canvas(title)
                .description(description)
                .build();
    }

    public static Faq fromName(String input) {
        try {
            return valueOf(input.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
