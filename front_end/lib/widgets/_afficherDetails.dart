import 'dart:ui';
import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';

class TransmissionDetailsDialog extends StatelessWidget {
  final Transmission item;

  const TransmissionDetailsDialog({super.key, required this.item});

  @override
  Widget build(BuildContext context) {
    return BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 5.0, sigmaY: 5.0),
      child: Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24.0)),
        elevation: 8.0,
        child: Container(
          constraints: const BoxConstraints(maxWidth: 400),
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 24.0),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Détails : ${item.nom}",
                  style: const TextStyle(fontSize: 20.0, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 16.0),
                _buildDetailRow("Provenance", "${item.provenance} ${item.provenanceResult != null ? '(${item.provenanceResult})' : ''}"),
                _buildDetailRow("Type", item.type),
                if (item.type == "déposition") _buildDetailRow("Responsable", item.responsable ?? '-'),
                _buildDetailRow("Objet", item.details),
                _buildDetailRow("Quantité", "${item.quantite}"),
                const SizedBox(height: 12.0),
                _buildDetailRow("Commentaire", item.details),
                const SizedBox(height: 16.0),
                const Divider(height: 1, color: Colors.grey),
                const SizedBox(height: 12.0),
                Text(
                  "Statut : ${item.estTerminee ? (item.type == "prêt" ? "Rendu - Terminée" : "Récupéré - Terminée") : "En cours"}",
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    color: item.estTerminee ? Colors.green : Colors.orange,
                  ),
                ),
                const SizedBox(height: 8.0),
                if (item.etat != null) _buildDetailRow('Etat', item.etat!),
                if (item.remarque != null && item.remarque!.isNotEmpty) _buildDetailRow('Remarque', item.remarque!),
                const SizedBox(height: 24.0),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.black87,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () => Navigator.pop(context),
                    child: const Text("Fermer"),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: TextStyle(fontSize: 12.0, color: Colors.grey[500], fontWeight: FontWeight.w500)),
          const SizedBox(height: 4.0),
          Text(value, style: const TextStyle(fontSize: 14.0, color: Colors.black87, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}