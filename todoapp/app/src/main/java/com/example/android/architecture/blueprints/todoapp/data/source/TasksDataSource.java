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

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;

import java.util.List;

/**
 * Main entry point for accessing tasks data.
 * <p>
 * For simplicity, only getTasks() and getTask() have callbacks. Consider adding callbacks to other
 * methods to inform the user of network/database errors or successful operations.
 * For example, when a new task is created, it's synchronously stored in cache but usually every
 * operation on database or network should be executed in a different thread.
 */
public interface TasksDataSource {

    /**
     * 获取Task列表数据结果的回调接口, 数据可能来自于远程数据源（如：服务器数据）,
     * 或本地数据源（如：数据库中缓存的数据）
     */
    interface LoadTasksCallback {
        /**
         * 当数据获取成功后的回调(对于远程数据源来说是网络通信正常,
         * 对于本地数据源来说是从本地成功获取到非脏的数据)
         * @param tasks 从数据源成功获取到的数据
         */
        void onTasksLoaded(List<Task> tasks);

        /**
         * 当数据获取失败后的回调(对于远程数据源来说是网络通信失败,
         * 对于本地数据源来说是本地无数据或虽有数据但都是脏数据)
         */
        void onDataNotAvailable();
    }

    interface GetTaskCallback {

        void onTaskLoaded(Task task);

        void onDataNotAvailable();
    }

    void getTasks(@NonNull LoadTasksCallback callback);

    void getTask(@NonNull String taskId, @NonNull GetTaskCallback callback);

    void saveTask(@NonNull Task task);

    void completeTask(@NonNull Task task);

    void completeTask(@NonNull String taskId);

    void activateTask(@NonNull Task task);

    void activateTask(@NonNull String taskId);

    void clearCompletedTasks();

    void refreshTasks();

    /**
     * 删除全部数据
     */
    void deleteAllTasks();

    /**
     * 删除一条数据
     * @param taskId
     */
    void deleteTask(@NonNull String taskId);
}
