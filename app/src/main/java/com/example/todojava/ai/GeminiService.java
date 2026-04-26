package com.example.todojava.ai;

import android.util.Log;

import com.example.todojava.tasks.Task;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public class GeminiService {
    private static final String TAG = "GeminiService";
    private static final String API_KEY = "AIzaSyAyxqd7DagTkNS2fD_hCwGD-x0X_lzd1KQ";
    private final GenerativeModelFutures model;

    public GeminiService() {
        GenerationConfig config = new GenerationConfig.Builder()
                .build();

        GenerativeModel gm = new GenerativeModel(
                "gemini-3-flash-preview",
                API_KEY,
                config
        );
        this.model = GenerativeModelFutures.from(gm);
    }

    public ListenableFuture<GenerateContentResponse> getPlan(List<Task> currentTasks, String instruction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("IMPORTANT: You are an API backend. Respond ONLY with raw JSON. No markdown, no triple backticks.\n");
        prompt.append("You are a personal task manager AI. You can create, update, and delete tasks.\n");
        prompt.append("Current tasks (with documentId):\n");
        
        for (Task task : currentTasks) {
            prompt.append("- ID: ").append(task.getDocumentId())
                  .append(", Title: ").append(task.getTitle())
                  .append(", Type: ").append(task.getType())
                  .append(", Due: ").append(task.getDueDate())
                  .append(", Completed: ").append(task.isCompleted())
                  .append("\n");
        }

        prompt.append("\nUser Instruction: ").append(instruction).append("\n");
        prompt.append("\nRequired JSON structure:\n");
        prompt.append("{\n");
        prompt.append("  \"message\": \"A brief explanation of what you plan to do.\",\n");
        prompt.append("  \"actions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"CREATE | UPDATE | DELETE\",\n");
        prompt.append("      \"id\": \"The documentId (for UPDATE/DELETE)\",\n");
        prompt.append("      \"task\": { \"title\": \"string\", \"type\": \"task|event\", \"dueDate\": \"string\", \"details\": \"string\", \"completed\": boolean }\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}");

        Content content = new Content.Builder()
                .addText(prompt.toString())
                .build();

        return model.generateContent(content);
    }
}
