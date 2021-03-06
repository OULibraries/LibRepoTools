/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.job;

import java.util.List;

/**
 *
 * @author Tao Zhao
 */
public interface JobQueueService {
    public boolean isJobQueueEmpty(String queueName);
    public List getJobQueueByName(String queueName);
    public void addJobIntoQueue(long jobId, String queueName);
    public long removeJobFromQueue(String queueName);
}
