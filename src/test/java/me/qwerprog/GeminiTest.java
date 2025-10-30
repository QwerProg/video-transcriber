package me.qwerprog;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiTest {
    public static void main(String[] args) {
        Client client = Client.builder().apiKey("AIzaSyDWwwhJN_dmHH-_nZY_J1DRUIHZf5diMGw").build();

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        "你好介绍一下你自己",
                        null);

        System.out.println(response.text());
    }
}