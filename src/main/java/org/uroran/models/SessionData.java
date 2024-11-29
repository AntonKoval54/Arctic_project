package org.uroran.models;

import lombok.*;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionData {
    private String name;
    private String host;
    private int port;
    private String user;
    private String pathToKey;
    private String passPhrase;
}
