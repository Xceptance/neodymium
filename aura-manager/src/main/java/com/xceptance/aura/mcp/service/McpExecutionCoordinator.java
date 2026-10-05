package com.xceptance.aura.mcp.service;

import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;

/**
 * Coordinates asynchronous execution requests from MCP agents, managing a thread-safe
 * FIFO queue for pending runs and dispatching sequentially to AuraQueueService.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class McpExecutionCoordinator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(McpExecutionCoordinator.class);

    private final AuraQueueService queueService;

    @Value("${server.port:8080}")
    private int serverPort = 8080;

    private final AtomicLong runCounter = new AtomicLong(0);

    private final Queue<PendingRun> pendingQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentMap<String, PendingRun> runRegistry = new ConcurrentHashMap<>();

    public McpExecutionCoordinator(final AuraQueueService queueService)
    {
        this.queueService = queueService;
        this.queueService.addRunCompletedListener(this::onActiveRunCompleted);
    }

    /**
     * Submits a test playbook for execution.
     * Returns immediately with RUNNING if the queue was idle, or ENQUEUED if another run is in progress.
     *
     * @param testPath the playbook file path
     * @param browserProfile target browser profile, if any
     * @param dataset optional dataset ID
     * @return DispatchResult containing runId, status, and queue position
     */
    public synchronized DispatchResult submitRun(final String testPath, final String browserProfile, final String dataset)
    {
        final String runId = generateRunId();

        final RunRequest req = new RunRequest();
        req.targetRunId = runId;
        final DatasetSelection sel = new DatasetSelection();
        sel.file = testPath;
        sel.id = dataset;
        if (browserProfile != null && !browserProfile.isBlank())
        {
            sel.browserProfiles = List.of(browserProfile);
            req.globalBrowserProfiles = List.of(browserProfile);
        }
        req.datasets = List.of(sel);
        req.headless = true;
        req.interactive = false;

        final PendingRun pendingRun = new PendingRun(runId, testPath, req);
        runRegistry.put(runId, pendingRun);

        if (!queueService.isRunningQueue())
        {
            LOGGER.info("[Aura MCP] Queue idle. Starting immediate execution of run '{}' ({})", runId, testPath);
            pendingRun.setStatus("RUNNING");
            queueService.executeQueue(req, getEffectiveServerPort());
            return new DispatchResult(runId, "RUNNING", 0, "Test execution started immediately.");
        }

        pendingQueue.add(pendingRun);
        final int position = pendingQueue.size();
        LOGGER.info("[Aura MCP] Queue busy. Enqueued run '{}' at position {}", runId, position);
        pendingRun.setStatus("ENQUEUED");
        return new DispatchResult(runId, "ENQUEUED", position, "Queue currently busy with active run. Request enqueued.");
    }

    /**
     * Submits a suite of multiple test playbooks for batched execution.
     *
     * @param testPaths list of playbook file paths
     * @param browserProfile target browser profile, if any
     * @return DispatchResult containing runId, status, and queue position
     */
    public synchronized DispatchResult submitSuite(final List<String> testPaths, final String browserProfile)
    {
        final String runId = generateRunId();

        final RunRequest req = new RunRequest();
        req.targetRunId = runId;
        final java.util.List<DatasetSelection> selections = new java.util.ArrayList<>();
        for (final String path : testPaths)
        {
            final DatasetSelection sel = new DatasetSelection();
            sel.file = path;
            if (browserProfile != null && !browserProfile.isBlank())
            {
                sel.browserProfiles = List.of(browserProfile);
            }
            selections.add(sel);
        }

        if (browserProfile != null && !browserProfile.isBlank())
        {
            req.globalBrowserProfiles = List.of(browserProfile);
        }
        req.datasets = selections;
        req.headless = true;
        req.interactive = false;

        final String suiteDescription = "Suite of " + testPaths.size() + " tests";
        final PendingRun pendingRun = new PendingRun(runId, suiteDescription, req);
        runRegistry.put(runId, pendingRun);

        if (!queueService.isRunningQueue())
        {
            LOGGER.info("[Aura MCP] Queue idle. Starting immediate execution of suite run '{}' ({} tests)", runId, testPaths.size());
            pendingRun.setStatus("RUNNING");
            queueService.executeQueue(req, getEffectiveServerPort());
            return new DispatchResult(runId, "RUNNING", 0, "Suite execution started immediately with " + testPaths.size() + " tests.");
        }

        pendingQueue.add(pendingRun);
        final int position = pendingQueue.size();
        LOGGER.info("[Aura MCP] Queue busy. Enqueued suite run '{}' at position {}", runId, position);
        pendingRun.setStatus("ENQUEUED");
        return new DispatchResult(runId, "ENQUEUED", position, "Queue currently busy with active run. Suite enqueued at position " + position + ".");
    }

    /**
     * Cancels an enqueued or actively running execution.
     * If the run is waiting in the pending queue, it is immediately removed without executing.
     * If the run is actively executing, the process is terminated.
     *
     * @param runId the run ID to cancel
     * @return true if cancelled, false if run was not found or already completed
     */
    public synchronized boolean cancelRun(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            return false;
        }

        final PendingRun run = runRegistry.get(runId);
        if (run != null && "ENQUEUED".equalsIgnoreCase(run.getStatus()))
        {
            pendingQueue.remove(run);
            run.setStatus("CANCELLED");
            LOGGER.info("[Aura MCP] Purged enqueued run '{}' from pending queue.", runId);
            return true;
        }

        final String activeRunId = queueService.getCurrentRunId();
        if (queueService.isRunningQueue() && runId.equals(activeRunId))
        {
            if (run != null)
            {
                run.setStatus("CANCELLED");
            }
            LOGGER.info("[Aura MCP] Aborting active run '{}' via queueService.", runId);
            queueService.stopCurrentTest();
            return true;
        }

        return false;
    }

    /**
     * Inspects the status of a run submitted through MCP.
     *
     * @param runId the run ID
     * @return current status string, or null if not found
     */
    public String getRunStatus(final String runId)
    {
        final PendingRun run = runRegistry.get(runId);
        if (run != null)
        {
            return run.getStatus();
        }
        return null;
    }

    private synchronized void onActiveRunCompleted(final String completedRunId)
    {
        LOGGER.info("[Aura MCP] Active test run '{}' completed. Inspecting pending queue...", completedRunId);

        final PendingRun finishedRun = runRegistry.get(completedRunId);
        if (finishedRun != null && !"CANCELLED".equalsIgnoreCase(finishedRun.getStatus()))
        {
            finishedRun.setStatus("COMPLETED");
        }

        final PendingRun next = pendingQueue.poll();
        if (next != null)
        {
            LOGGER.info("[Aura MCP] Dequeueing and executing next pending run '{}' ({})", next.getRunId(), next.getTestPath());
            next.setStatus("RUNNING");
            queueService.executeQueue(next.getRunRequest(), getEffectiveServerPort());
        }
        else
        {
            LOGGER.info("[Aura MCP] No more pending test runs in MCP queue.");
        }
    }

    private int getEffectiveServerPort()
    {
        return this.serverPort <= 0 ? 8080 : this.serverPort;
    }

    private String generateRunId()
    {
        final long seq = runCounter.incrementAndGet() % 1000;
        return "run_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + String.format("%03d", seq);
    }

    public static class DispatchResult
    {
        private final String runId;
        private final String status;
        private final int queuePosition;
        private final String message;

        public DispatchResult(final String runId, final String status, final int queuePosition, final String message)
        {
            this.runId = runId;
            this.status = status;
            this.queuePosition = queuePosition;
            this.message = message;
        }

        public String getRunId()
        {
            return runId;
        }

        public String getStatus()
        {
            return status;
        }

        public int getQueuePosition()
        {
            return queuePosition;
        }

        public String getMessage()
        {
            return message;
        }
    }

    private static class PendingRun
    {
        private final String runId;
        private final String testPath;
        private final RunRequest runRequest;
        private volatile String status = "ENQUEUED";

        public PendingRun(final String runId, final String testPath, final RunRequest runRequest)
        {
            this.runId = runId;
            this.testPath = testPath;
            this.runRequest = runRequest;
        }

        public String getRunId()
        {
            return runId;
        }

        public String getTestPath()
        {
            return testPath;
        }

        public RunRequest getRunRequest()
        {
            return runRequest;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(final String status)
        {
            this.status = status;
        }
    }
}
