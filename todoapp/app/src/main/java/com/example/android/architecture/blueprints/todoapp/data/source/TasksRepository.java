/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source;

import static com.google.common.base.Preconditions.checkNotNull;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 * <p>
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
public class TasksRepository implements TasksDataSource {

    private static TasksRepository INSTANCE = null;

    private final TasksDataSource mTasksRemoteDataSource;

    private final TasksDataSource mTasksLocalDataSource;

    /**
     * 内存中的tasks缓存. key-taskId, value: Task对象
     * This variable has package local visibility so it can be accessed from tests.
     */
    Map<String, Task> mCachedTasks;

    /**
     * 标记内存缓存中的tasks数据是否是脏数据的标志位. 如果不是脏数据, 则数据的请求可以直接
     * 使用内存缓存的数据. 如果是脏数据, 则内存缓存的数据不能使用, 必须要从数据源(远程或本
     * 地)重新获取后才能使用, 并且重新获取后即可修改该标志位的状态.
     *
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    boolean mCacheIsDirty = false;

    // Prevent direct instantiation.
    private TasksRepository(@NonNull TasksDataSource tasksRemoteDataSource,
                            @NonNull TasksDataSource tasksLocalDataSource) {
        mTasksRemoteDataSource = checkNotNull(tasksRemoteDataSource);
        mTasksLocalDataSource = checkNotNull(tasksLocalDataSource);
    }

    /**
     * Returns the single instance of this class, creating it if necessary.
     *
     * @param tasksRemoteDataSource the backend data source
     * @param tasksLocalDataSource  the device storage data source
     * @return the {@link TasksRepository} instance
     */
    public static TasksRepository getInstance(TasksDataSource tasksRemoteDataSource,
                                              TasksDataSource tasksLocalDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new TasksRepository(tasksRemoteDataSource, tasksLocalDataSource);
        }
        return INSTANCE;
    }

    /**
     * Used to force {@link #getInstance(TasksDataSource, TasksDataSource)} to create a new instance
     * next time it's called.
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }

    /**
     * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     * <p>
     * Note: {@link LoadTasksCallback#onDataNotAvailable()} is fired if all data sources fail to
     * get the data.
     */
    @Override
    public void getTasks(@NonNull final LoadTasksCallback callback) {
        checkNotNull(callback);

        // Respond immediately with cache if available and not dirty
        /// 如果内存中存在缓存数据并且该数据不是脏数据
        if (mCachedTasks != null && !mCacheIsDirty) {
            callback.onTasksLoaded(new ArrayList<>(mCachedTasks.values()));
            return;
        }
        /// 缓存是脏数据, 必须从数据源获取, 获取后需要刷新/更新内存缓存
        if (mCacheIsDirty) {
            // If the cache is dirty we need to fetch new data from the network.
            getTasksFromRemoteDataSource(callback);
        } else {
            /// 缓存不是脏数据, 但缓存中却没有数据, 必须从数据源获取, 获取后需要提供给内存缓存.
            /// 先尝试从本地数据源获取, 如果能获取到, 就直接提供给需求方(Presenter). 否则
            /// 就需要再从远程数据源去获取, 获取完之后需要把数据既提供给需求方(Presenter),
            /// 又需要把数据提供给本地数据源存储, 还需要提供给内存缓存供刷新.
            // Query the local storage if available. If not, query the network.
            getTasksFromLocalDataSource(callback);
        }
    }

    @Override
    public void saveTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.saveTask(task);
        mTasksLocalDataSource.saveTask(task);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(), task);
    }

    @Override
    public void completeTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.completeTask(task);
        mTasksLocalDataSource.completeTask(task);

        Task completedTask = new Task(task.getTitle(), task.getDescription(), task.getId(), true);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(), completedTask);
    }

    @Override
    public void completeTask(@NonNull String taskId) {
        checkNotNull(taskId);
        completeTask(getTaskWithId(taskId));
    }

    @Override
    public void activateTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.activateTask(task);
        mTasksLocalDataSource.activateTask(task);

        Task activeTask = new Task(task.getTitle(), task.getDescription(), task.getId());

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.put(task.getId(), activeTask);
    }

    @Override
    public void activateTask(@NonNull String taskId) {
        checkNotNull(taskId);
        activateTask(getTaskWithId(taskId));
    }

    @Override
    public void clearCompletedTasks() {
        mTasksRemoteDataSource.clearCompletedTasks();
        mTasksLocalDataSource.clearCompletedTasks();

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        Iterator<Map.Entry<String, Task>> it = mCachedTasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Task> entry = it.next();
            if (entry.getValue().isCompleted()) {
                it.remove();
            }
        }
    }

    /**
     * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     * <p>
     * Note: {@link GetTaskCallback#onDataNotAvailable()} is fired if both data sources fail to
     * get the data.
     */
    @Override
    public void getTask(@NonNull final String taskId, @NonNull final GetTaskCallback callback) {
        checkNotNull(taskId);
        checkNotNull(callback);

        Task cachedTask = getTaskWithId(taskId);

        // Respond immediately with cache if available
        if (cachedTask != null) {
            callback.onTaskLoaded(cachedTask);
            return;
        }

        // Load from server/persisted if needed.

        // Is the task in the local data source? If not, query the network.
        mTasksLocalDataSource.getTask(taskId, new GetTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                // Do in memory cache update to keep the app UI up to date
                if (mCachedTasks == null) {
                    mCachedTasks = new LinkedHashMap<>();
                }
                mCachedTasks.put(task.getId(), task);
                callback.onTaskLoaded(task);
            }

            @Override
            public void onDataNotAvailable() {
                mTasksRemoteDataSource.getTask(taskId, new GetTaskCallback() {
                    @Override
                    public void onTaskLoaded(Task task) {
                        // Do in memory cache update to keep the app UI up to date
                        if (mCachedTasks == null) {
                            mCachedTasks = new LinkedHashMap<>();
                        }
                        mCachedTasks.put(task.getId(), task);
                        callback.onTaskLoaded(task);
                    }

                    @Override
                    public void onDataNotAvailable() {
                        callback.onDataNotAvailable();
                    }
                });
            }
        });
    }

    @Override
    public void refreshTasks() {
        mCacheIsDirty = true;
    }

    /**
     * 删除全部数据
     */
    @Override
    public void deleteAllTasks() {
        mTasksRemoteDataSource.deleteAllTasks();
        mTasksLocalDataSource.deleteAllTasks();

        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.clear();
    }

    /**
     * 删除一条数据
     * @param taskId
     */
    @Override
    public void deleteTask(@NonNull String taskId) {
        mTasksRemoteDataSource.deleteTask(checkNotNull(taskId));
        mTasksLocalDataSource.deleteTask(checkNotNull(taskId));

        mCachedTasks.remove(taskId);
    }

    /**
     * 从本地数据源获取数据
     * @param callback 数据获取结果的回调接口
     */
    private void getTasksFromLocalDataSource(final LoadTasksCallback callback) {
        mTasksLocalDataSource.getTasks(new LoadTasksCallback() {
            /**
             * 从本地数据源成功获取到数据后的回调
             * @param tasks 从数据源成功获取到的数据
             */
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // 把数据提供给内存缓存供刷新
                refreshCache(tasks);
                // 把数据既提供给需求方(Presenter)
                callback.onTasksLoaded(new ArrayList<>(mCachedTasks.values()));
            }

            /**
             * 从本地数据源获取不到数据(包括发生异常)后的回调
             */
            @Override
            public void onDataNotAvailable() {
                // 本地数据源获取数据失败, 但是还没到放弃的时候, 还可以委托
                // 远程数据源, 看看它那里能否提供协助实现此需求
                getTasksFromRemoteDataSource(callback);
            }
        });
    }

    /**
     * 从远程数据源获取数据
     * @param callback 数据获取结果的回调接口
     */
    private void getTasksFromRemoteDataSource(@NonNull final LoadTasksCallback callback) {
        mTasksRemoteDataSource.getTasks(new LoadTasksCallback() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                // 把数据提供给内存缓存供刷新
                refreshCache(tasks);
                // 把数据提供给本地数据源存储
                refreshLocalDataSource(tasks);
                // 把数据既提供给需求方(Presenter)
                callback.onTasksLoaded(new ArrayList<>(mCachedTasks.values()));
            }

            @Override
            public void onDataNotAvailable() {
                // 通知需求方(Presenter), 数据获取失败
                callback.onDataNotAvailable();
            }
        });
    }

    /**
     * 刷新/更新内存里的缓存数据, 通常都是当从某个数据源(远程或本地)拉取数据后就立即调用该方法
     */
    private void refreshCache(List<Task> tasks) {
        if (mCachedTasks == null) {
            mCachedTasks = new LinkedHashMap<>();
        }
        mCachedTasks.clear();
        for (Task task : tasks) {
            mCachedTasks.put(task.getId(), task);
        }
        mCacheIsDirty = false;
    }

    /**
     * 刷新/更新本地数据源中的数据
     * @param tasks
     */
    private void refreshLocalDataSource(List<Task> tasks) {
        mTasksLocalDataSource.deleteAllTasks();
        for (Task task : tasks) {
            mTasksLocalDataSource.saveTask(task);
        }
    }

    @Nullable
    private Task getTaskWithId(@NonNull String id) {
        checkNotNull(id);
        if (mCachedTasks == null || mCachedTasks.isEmpty()) {
            return null;
        } else {
            return mCachedTasks.get(id);
        }
    }
}
