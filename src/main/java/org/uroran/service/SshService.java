package org.uroran.service;

import com.jcraft.jsch.*;
import org.uroran.models.SessionData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SshService {
    private Session session;
    private ChannelShell channelShell;
    private InputStream inputStream;
    private OutputStream outputStream;

    private final SessionData sessionData;

    public SshService(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    public void connect() throws JSchException, IOException {
        JSch jsch = new JSch();

        jsch.addIdentity(sessionData.getPathToKey(), sessionData.getPassPhrase());
        session = jsch.getSession(sessionData.getUser(), sessionData.getHost(), sessionData.getPort());
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        channelShell = (ChannelShell) session.openChannel("shell");

        inputStream = channelShell.getInputStream();
        outputStream = channelShell.getOutputStream();

        channelShell.connect();
    }

    public String sendCommand(String command) throws Exception {
        if (channelShell == null || !channelShell.isConnected()) {
            throw new IllegalStateException("Shell channel is not connected.");
        }

        outputStream.write((command + "\n").getBytes());
        outputStream.flush();

        Thread.sleep(500);
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[2048];
        while (inputStream.available() > 0) {
            int bytesRead = inputStream.read(buffer);
            output.append(new String(buffer, 0, bytesRead));
        }

        return output.toString();
    }

    public void disconnect() {
        if (channelShell != null && channelShell.isConnected()) {
            channelShell.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        var sessionToLoad = SessionData.builder()
                .name("uroran")
                .host("umt.imm.uran.ru")
                .port(22)
                .user("s0116")
                .pathToKey("src/main/resources/keys/key")
                .passPhrase("pp5sem")
                .build();
        SshService sshService = new SshService(sessionToLoad);

        sshService.connect();
        Thread.sleep(2000);
        System.out.println(sshService.sendCommand("ls"));
    }
}
