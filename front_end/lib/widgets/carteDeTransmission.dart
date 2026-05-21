import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';

class TransmissionCard extends StatelessWidget {
  final Transmission item;
  final VoidCallback onDelete;
  final VoidCallback onShowDetails;
  final VoidCallback onToggleStatus;

  const TransmissionCard({
    super.key,
    required this.item,
    required this.onDelete,
    required this.onShowDetails,
    required this.onToggleStatus,
  });

  @override
  Widget build(BuildContext context) {
    final bool isExterne = item.provenance == 'externe';
    final Color accentColor = isExterne ? Colors.red.shade700 : Colors.amber.shade700;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16.0),
          border: Border.all(color: Colors.grey.shade200),
          boxShadow: const [
            BoxShadow(color: Colors.black12, blurRadius: 14, offset: Offset(0, 8)),
          ],
        ),
        child: ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 14.0),
          leading: Container(
            width: 8,
            decoration: BoxDecoration(color: accentColor, borderRadius: BorderRadius.circular(8.0)),
          ),
          title: Text(item.nom, style: TextStyle(fontWeight: FontWeight.w700, color: Colors.grey[900])),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text("Le ${item.date.day}/${item.date.month}/${item.date.year}", style: TextStyle(color: Colors.grey[700])),
              const SizedBox(height: 6),
              Wrap(
                spacing: 8,
                runSpacing: 4,
                children: [
                  Text(
                    item.isSynced ? "— Sauvegardée" : "— Hors ligne",
                    style: TextStyle(color: item.isSynced ? Colors.green : Colors.red, fontWeight: FontWeight.bold),
                  ),
                  if (item.estTerminee)
                    Text(
                      item.type == "prêt" ? "— Rendu - Terminée" : "— Récupéré - Terminée",
                      style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold),
                    ),
                ],
              ),
              const SizedBox(height: 6),
              Text(isExterne ? 'Type : externe' : 'Type : interne', style: TextStyle(color: accentColor, fontWeight: FontWeight.w600)),
            ],
          ),
          trailing: PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'delete') onDelete();
              if (value == 'show') onShowDetails();
              if (value == 'toggle_status') onToggleStatus();
            },
            itemBuilder: (context) => [
              const PopupMenuItem(value: 'show', child: ListTile(leading: Icon(Icons.visibility), title: Text('Afficher'))),
              if (!item.estTerminee)
                PopupMenuItem(
                  value: 'toggle_status',
                  child: ListTile(
                    leading: Icon(item.type == "prêt" ? Icons.assignment_return : Icons.get_app),
                    title: Text(item.type == "prêt" ? "Rendre" : "Récupérer"),
                  ),
                ),
              const PopupMenuItem(value: 'delete', child: ListTile(leading: Icon(Icons.delete, color: Colors.red), title: Text('Supprimer', style: TextStyle(color: Colors.red)))),
            ],
            icon: const Icon(Icons.more_vert),
          ),
        ),
      ),
    );
  }
}