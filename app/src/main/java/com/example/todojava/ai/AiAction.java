package com.example.todojava.ai;

import com.example.todojava.tasks.Task;
import java.util.ArrayList;
import java.util.List;

public class AiAction {
    public enum Type { CREATE, UPDATE, DELETE }

    private Type type;
    private Task task;
    private String originalTaskId; // For UPDATE and DELETE

    public AiAction(Type type, Task task) {
        this.type = type;
        this.task = task;
    }

    public AiAction(Type type, Task task, String originalTaskId) {
        this.type = type;
        this.task = task;
        this.originalTaskId = originalTaskId;
    }

    public Type getType() { return type; }
    public Task getTask() { return task; }
    public String getOriginalTaskId() { return originalTaskId; }
}
