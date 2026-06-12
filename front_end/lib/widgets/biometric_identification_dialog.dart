import 'package:flutter/material.dart';
import '../services/biometric_service.dart';

class BiometricIdentificationDialog extends StatefulWidget {
  const BiometricIdentificationDialog({super.key});

  @override
  State<BiometricIdentificationDialog> createState() => _BiometricIdentificationDialogState();
}

class _BiometricIdentificationDialogState extends State<BiometricIdentificationDialog> {
  bool isProcessing = false;
  bool identificationSuccess = false;
  String statusMessage = 'Prêt pour l\'identification';

  Future<void> _startIdentification() async {
    setState(() {
      isProcessing = true;
      statusMessage = 'Chargement des données biométriques...';
    });

    try {
      // Load biometric data from backend first
      final dataLoaded = await BiometricService.loadBiometricData();
      
      if (!dataLoaded) {
        setState(() {
          isProcessing = false;
          statusMessage = 'Erreur: serveur';
        });
        return;
      }

      setState(() {
        statusMessage = 'Données chargées. Ouverture du lecteur ZKTeco 9500...';
      });

      final deviceOpened = await BiometricService.prepareZKDevice();
      if (!deviceOpened) {
        final errorDetail = await BiometricService.getZKError();
        setState(() {
          isProcessing = false;
          statusMessage = errorDetail != null && errorDetail.isNotEmpty
            ? 'Erreur: $errorDetail'
            : 'Erreur: impossible d\'ouvrir le lecteur ZKTeco 9500';
        });
        return;
      }

      setState(() {
        statusMessage = 'Lecture du doigt en cours... placez votre doigt sur le lecteur';
      });

      // Perform fingerprint identification
      final result = await BiometricService.identifyFingerprint();

      if (!mounted) return;

      if (result) {
        // Clear biometric data from memory
        await BiometricService.clearBiometricData();
        
        setState(() {
          isProcessing = false;
          identificationSuccess = true;
          statusMessage = 'Identification réussie!';
        });
      } else {
        setState(() {
          isProcessing = false;
          statusMessage = 'Identification échouée. Veuillez réessayer.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          isProcessing = false;
          statusMessage = 'Erreur: ${e.toString()}';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Identification Biométrique'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isProcessing)
              Column(
                children: [
                  const CircularProgressIndicator(),
                  const SizedBox(height: 16),
                  Text(statusMessage, textAlign: TextAlign.center),
                ],
              )
            else if (identificationSuccess)
              Column(
                children: [
                  const Icon(
                    Icons.check_circle,
                    color: Colors.green,
                    size: 64,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    statusMessage,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      color: Colors.green,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              )
            else
              Column(
                children: [
                  const Icon(
                    Icons.fingerprint,
                    size: 64,
                    color: Colors.blue,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    statusMessage,
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
          ],
        ),
      ),
      actions: [
        if (!isProcessing && !identificationSuccess)
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Annuler'),
          ),
        if (!isProcessing && !identificationSuccess)
          ElevatedButton(
            onPressed: _startIdentification,
            child: const Text('Débuter'),
          ),
        if (identificationSuccess)
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Terminer'),
          ),
      ],
    );
  }
}
