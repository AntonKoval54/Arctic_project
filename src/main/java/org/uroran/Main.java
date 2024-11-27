package org.uroran;

import org.uroran.gui.SessionManager;
import org.uroran.service.SessionDataService;

import javax.swing.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
//

//        SessionDataService sessionDataService = new SessionDataService();
//        sessionDataService.saveSessionData(sessionToLoad);
//        SessionData sessionToRetrieve = sessionDataService.loadSessionData("uroran");

//        SshService sshService = new SshService(sessionToRetrieve);
//        sshService.connect();
//        System.out.println(sshService.sendCommand(""));
//        System.out.println(sshService.sendCommand(""));
//        System.out.println(sshService.sendCommand("mps"));

        SessionDataService sessionDataService = new SessionDataService();

        SwingUtilities.invokeLater(() -> {
            SessionManager manager = new SessionManager(sessionDataService);
            manager.setVisible(true);
        });

    }
}