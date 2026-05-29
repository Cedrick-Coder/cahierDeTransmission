package com.zkteco.biometric;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class ZKFPDemo extends JFrame {
    private final JButton btnOpen;
    private final JButton btnEnroll;
    private final JButton btnVerify;
    private final JButton btnIdentify;
    private final JButton btnRegImg;
    private final JButton btnIdentImg;
    private final JButton btnClose;
    private final JButton btnImg;
    private final JTextArea textArea;

    private final ZKDeviceManager deviceManager;

    public ZKFPDemo() {
        setTitle("ZKFinger Demo");
        setLayout(null);

        btnOpen = new JButton("Open");
        btnOpen.setBounds(30, 30, 100, 30);
        add(btnOpen);

        btnEnroll = new JButton("Enroll");
        btnEnroll.setBounds(30, 80, 100, 30);
        add(btnEnroll);

        btnVerify = new JButton("Verify");
        btnVerify.setBounds(30, 130, 100, 30);
        add(btnVerify);

        btnIdentify = new JButton("Identify");
        btnIdentify.setBounds(30, 180, 100, 30);
        add(btnIdentify);

        btnRegImg = new JButton("Register By Image");
        btnRegImg.setBounds(15, 230, 120, 30);
        add(btnRegImg);

        btnIdentImg = new JButton("Verify By Image");
        btnIdentImg.setBounds(15, 280, 120, 30);
        add(btnIdentImg);

        btnClose = new JButton("Close");
        btnClose.setBounds(30, 330, 100, 30);
        add(btnClose);

        btnImg = new JButton();
        btnImg.setBounds(150, 10, 256, 300);
        btnImg.setDefaultCapable(false);
        add(btnImg);

        textArea = new JTextArea();
        textArea.setBounds(10, 440, 480, 100);
        add(textArea);

        setSize(520, 580);
        setLocationRelativeTo(null);
        setResizable(false);

        deviceManager = new ZKDeviceManager(new ZKDeviceManager.Listener() {
            @Override
            public void onStatus(String message) {
                textArea.setText(message);
            }

            @Override
            public void onImageCaptured(BufferedImage image) {
                btnImg.setIcon(new ImageIcon(image));
            }
        });

        addListeners();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                deviceManager.closeDevice();
            }
        });

        setVisible(true);
    }

    private void addListeners() {
        btnOpen.addActionListener(e -> deviceManager.openDevice());
        btnClose.addActionListener(e -> deviceManager.closeDevice());
        btnEnroll.addActionListener(e -> deviceManager.startEnrollment());
        btnVerify.addActionListener(e -> deviceManager.setVerifyMode());
        btnIdentify.addActionListener(e -> deviceManager.setIdentifyMode());
        btnRegImg.addActionListener(e -> deviceManager.registerFromImage());
        btnIdentImg.addActionListener(e -> deviceManager.verifyFromImage());
    }
}
