package com.zkteco.biometric;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ZKFPDemo extends JFrame {
    private static final String BACKEND_URL = "http://127.0.0.1:8000/api/personne-autorisee";
    private static final String BACKEND_DIR = "D:\\cahierDeTransmission\\backEnd";

    private final JPanel registrationPanel;
    private final JPanel enrollmentPanel;
    private final JTextField txtNom;
    private final JTextField txtPoste;
    private final JButton btnNext;

    private final JButton btnOpen;
    private final JButton btnEnroll;
    private final JButton btnClose;
    private final JButton btnBack;
    private final JButton btnFinishSend;
    private final JButton btnImg;
    private final JTextArea textArea;

    private final ZKDeviceManager deviceManager;
    private String currentTemplateBase64;
    private final BackendService backendService;

    public ZKFPDemo() {
        setTitle("ZKFinger - Enrôlement et envoi");
        setLayout(null);
        setResizable(false);

        registrationPanel = new JPanel(null);
        registrationPanel.setBounds(10, 10, 420, 140);
        registrationPanel.setBackground(new Color(240, 240, 240));

        JLabel lblTitle = new JLabel("1. Remplissez le formulaire puis cliquez sur Suivant");
        lblTitle.setBounds(10, 5, 400, 25);
        registrationPanel.add(lblTitle);

        JLabel lblNom = new JLabel("Nom :");
        lblNom.setBounds(10, 40, 80, 25);
        registrationPanel.add(lblNom);

        txtNom = new JTextField();
        txtNom.setBounds(100, 40, 300, 25);
        registrationPanel.add(txtNom);

        JLabel lblPoste = new JLabel("Poste :");
        lblPoste.setBounds(10, 75, 80, 25);
        registrationPanel.add(lblPoste);

        txtPoste = new JTextField();
        txtPoste.setBounds(100, 75, 300, 25);
        registrationPanel.add(txtPoste);

        btnNext = new JButton("Suivant");
        btnNext.setBounds(150, 110, 120, 25);
        registrationPanel.add(btnNext);
        add(registrationPanel);

        enrollmentPanel = new JPanel(null);
        enrollmentPanel.setBounds(10, 160, 420, 320);
        enrollmentPanel.setBackground(new Color(250, 250, 250));
        enrollmentPanel.setVisible(false);

        btnOpen = new JButton("Ouvrir lecteur");
        btnOpen.setBounds(10, 10, 130, 30);
        enrollmentPanel.add(btnOpen);

        btnEnroll = new JButton("Démarrer enrôlement");
        btnEnroll.setBounds(145, 10, 160, 30);
        enrollmentPanel.add(btnEnroll);

        btnClose = new JButton("Fermer lecteur");
        btnClose.setBounds(310, 10, 100, 30);
        enrollmentPanel.add(btnClose);

        btnBack = new JButton("Retour");
        btnBack.setBounds(10, 50, 100, 30);
        enrollmentPanel.add(btnBack);

        btnFinishSend = new JButton("Terminé et envoyer");
        btnFinishSend.setBounds(120, 50, 290, 30);
        btnFinishSend.setEnabled(false);
        enrollmentPanel.add(btnFinishSend);

        btnImg = new JButton();
        btnImg.setBounds(10, 90, 256, 240);
        btnImg.setDefaultCapable(false);
        enrollmentPanel.add(btnImg);

        textArea = new JTextArea();
        textArea.setBounds(276, 90, 134, 240);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        enrollmentPanel.add(textArea);

        add(enrollmentPanel);

        setSize(450, 520);
        setLocationRelativeTo(null);

        deviceManager = new ZKDeviceManager(new ZKDeviceManager.Listener() {
            @Override
            public void onStatus(String message) {
                textArea.setText(message);
            }

            @Override
            public void onImageCaptured(BufferedImage image) {
                btnImg.setIcon(new ImageIcon(image));
            }

            @Override
            public void onEnrollmentReady(String base64Template) {
                currentTemplateBase64 = base64Template;
                btnFinishSend.setEnabled(true);
                textArea.setText("Enrôlement terminé. Cliquez sur Terminé et envoyer.");
            }
        });

        backendService = new BackendService(BACKEND_DIR, BACKEND_URL);

        addListeners();

        // Back button: return to registration form without closing device
        btnBack.addActionListener(e -> {
            deviceManager.stopAcquisition();
            currentTemplateBase64 = null;
            btnFinishSend.setEnabled(false);
            enrollmentPanel.setVisible(false);
            registrationPanel.setVisible(true);
            textArea.setText("");
        });

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
        btnNext.addActionListener(e -> openEnrollmentPanel());
        btnOpen.addActionListener(e -> deviceManager.openDevice());
        btnClose.addActionListener(e -> deviceManager.closeDevice());
        btnEnroll.addActionListener(e -> {
            currentTemplateBase64 = null;
            btnFinishSend.setEnabled(false);
            deviceManager.startEnrollment();
        });
        btnFinishSend.addActionListener(e -> sendToBackend());
    }

    private void openEnrollmentPanel() {
        String nom = txtNom.getText().trim();
        String poste = txtPoste.getText().trim();

        if (nom.isEmpty() || poste.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez renseigner le nom et le poste avant de continuer.",
                    "Informations manquantes",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        registrationPanel.setVisible(false);
        enrollmentPanel.setVisible(true);
        textArea.setText("2. Branchez le lecteur, ouvrez-le, puis lancez l'enrôlement.");
    }

    private void sendToBackend() {
        if (currentTemplateBase64 == null || currentTemplateBase64.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucun modèle biométrique n'a été généré. Enregistrez d'abord votre empreinte.",
                    "Enrôlement incomplet",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nom = txtNom.getText().trim();
        String poste = txtPoste.getText().trim();

        if (nom.isEmpty() || poste.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Le nom et le poste sont requis.",
                    "Informations manquantes",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            HttpResponse<String> response = backendService.sendPerson(nom, poste, currentTemplateBase64);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                textArea.setText("Données envoyées avec succès au backend.");
                JOptionPane.showMessageDialog(this,
                        "Envoi réussi.",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                btnFinishSend.setEnabled(false);
            } else {
                textArea.setText("Échec de l'envoi au backend : code " + response.statusCode());
                JOptionPane.showMessageDialog(this,
                        "Échec de l'envoi au backend : " + response.statusCode(),
                        "Erreur réseau",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            textArea.setText("Erreur durant l'envoi : " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Erreur durant l'envoi : " + ex.getMessage(),
                    "Erreur réseau",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
