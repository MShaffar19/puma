package com.dianping.puma.syncserver.remote.receiver;

import com.dianping.puma.core.constant.ActionOperation;
import com.dianping.puma.core.entity.ShardSyncTask;
import com.dianping.puma.core.monitor.EventListener;
import com.dianping.puma.core.monitor.NotifyService;
import com.dianping.puma.core.monitor.event.Event;
import com.dianping.puma.core.monitor.event.ShardSyncTaskOperationEvent;
import com.dianping.puma.core.monitor.event.SyncTaskOperationEvent;
import com.dianping.puma.core.service.ShardSyncTaskService;
import com.dianping.puma.syncserver.job.container.TaskExecutorContainer;
import com.dianping.puma.syncserver.job.executor.TaskExecutionException;
import com.dianping.puma.syncserver.job.executor.TaskExecutor;
import com.dianping.puma.syncserver.job.executor.builder.TaskExecutorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("shardSyncTaskOperationReceiver")
public class ShardSyncTaskOperationReceiver implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ShardSyncTaskOperationReceiver.class);

    @Autowired
    ShardSyncTaskService shardSyncTaskService;

    @Autowired
    TaskExecutorBuilder taskExecutorBuilder;

    @Autowired
    TaskExecutorContainer taskExecutorContainer;

    @Autowired
    NotifyService notifyService;

    @Override
    public void onEvent(Event event) {
        if (event instanceof ShardSyncTaskOperationEvent) {
            LOG.info("Receive shard sync task operation event.");

            String taskName = ((ShardSyncTaskOperationEvent) event).getTaskName();
            ActionOperation operation = ((ShardSyncTaskOperationEvent) event).getOperation();

            switch (operation) {
                case CREATE:
                    ShardSyncTask syncTask = shardSyncTaskService.find(taskName);
                    TaskExecutor taskExecutor = taskExecutorBuilder.build(syncTask);

                    try {
                        taskExecutorContainer.submit(taskExecutor);
                    } catch (TaskExecutionException e) {
                        notifyService.alarm(e.getMessage(), e, false);
                    }
                    break;

                case REMOVE:
                    taskExecutorContainer.deleteShardSyncTask(taskName);
            }
        }
    }
}