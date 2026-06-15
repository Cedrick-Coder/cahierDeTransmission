import 'package:flutter/material.dart';
import '../constData/master_key.dart';
import '../modeleDEClasse/transmission.dart';
import 'package:front_end/services/transmission_repository.dart';

class TransmissionFinalizeDialog extends StatefulWidget {
  final Transmission item;
  final TransmissionRepository repository;

  const TransmissionFinalizeDialog({super.key, required this.item, required this.repository});

  @override
  State<TransmissionFinalizeDialog> createState() => _TransmissionFinalizeDialogState();
}

class _TransmissionFinalizeDialogState extends State<TransmissionFinalizeDialog> {
  late String selectedEtat;
  late TextEditingController remarqueController;
  late TextEditingController passwordController;

  @override
  void initState() {
    super.initState();
    selectedEtat = widget.item.etat ?? 'suivi';
    remarqueController = TextEditingController(text: widget.item.remarque ?? '');
    passwordController = TextEditingController();
  }

  @override
  void dispose() {
    remarqueController.dispose();
    passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.item.type == 'prêt' ? 'Rendre' : 'Récupérer'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          DropdownButtonFormField<String>(
            initialValue: selectedEtat,
            items: const [
              DropdownMenuItem(value: 'terminer', child: Text('TERMINER')),
              DropdownMenuItem(value: 'suivi', child: Text('SUIVI')),
            ],
            onChanged: (v) {
              if (v != null) setState(() => selectedEtat = v);
            },
            decoration: const InputDecoration(labelText: 'Etat'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: remarqueController,
            maxLength: 75,
            decoration: const InputDecoration(labelText: 'Remarque'),
          ),
          if (selectedEtat == 'terminer') ...[
            const SizedBox(height: 12),
            TextField(
              controller: passwordController,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Mot de passe',
                hintText: 'Entrez le mot de passe de confirmation',
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Annuler'),
        ),
        ElevatedButton(
          onPressed: () async {
            if (remarqueController.text.length > 75) {
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Remarque trop longue (75 caractères max)')));
              return;
            }

            if (selectedEtat == 'terminer') {
              final password = passwordController.text.trim();
              if (password != MasterKey.masterKEY) {
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mot de passe incorrect. Veuillez réessayer.')));
                return;
              }
            }

            final success = await widget.repository.finalizeTransmission(
              widget.item,
              selectedEtat,
              remarqueController.text.trim().isEmpty ? null : remarqueController.text.trim(),
            );

            if (!mounted) return;
            if (success) {
              final resultMessage = selectedEtat == 'terminer'
                  ? 'Transmission bien ${widget.item.type == 'prêt' ? 'rendue' : 'récupérée'} et terminée.'
                  : 'État enregistré avec succès.';
              ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(resultMessage)));
              Navigator.of(context).pop(true);
            } else {
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Échec de l’envoi au serveur. Veuillez réessayer.')));
            }
          },
          child: Text(selectedEtat == 'terminer' ? 'Suivant' : 'Terminé'),
        ),
      ],
    );
  }
}
