package org.dcache.pool.classic;

import diskCacheV111.vehicles.JobInfo;
import java.util.List;
import java.util.NoSuchElementException;
import org.dcache.util.IoPriority;

/**
 * @since 1.9.11
 */
public interface IoScheduler {

    /**
     * Add a new request to schedule.
     *
     * @param request to run.
     * @param priority of the request.
     * @return a sort hand identifier of the request
     */
    public int add(PoolIORequest request, IoPriority priority);

    /**
     * Cancel the request. Any IO in progress will be interrupted.
     * @param id
     * @throws NoSuchElementException
     */
    public void cancel(int id) throws NoSuchElementException;

    /**
     * Get the maximal number of concurrently running jobs by this scheduler.
     *
     * @return maximal number of jobs.
     */
    public int getMaxActiveJobs();

    /**
     * Get current number of concurrently running jobs.
     * @return number of running jobs.
     */
    public int getActiveJobs();

    /**
     * Get number of requests waiting for execution.
     * @return number of pending requests.
     */
    public int getQueueSize();

    /**
     * Set maximal number of concurrently running jobs by this scheduler. All pending
     * jobs will be executed.
     * @param max
     */
    public void setMaxActiveJobs(int max);

    /**
     * Get the name of this scheduler.
     * @return name of the scheduler
     */
    public String getName();

    /**
     * Shutdown the scheduler. All subsequent execution request will be rejected.
     */
    public void shutdown();

    // legacy crap
    public List<JobInfo> getJobInfos();

    public PoolIORequest getJobInfo(int id);

    public StringBuffer printJobQueue(StringBuffer sb);
}