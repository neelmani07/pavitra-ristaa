package com.pavitraristaa.support.dto;

import java.util.List;

public record HelpResponse(List<HelpTopic> topics) {

    public record HelpTopic(String title, String body) {
    }
}
