// ignore_for_file: unused_local_variable, file_names

import 'package:front_end/services/api_services.dart';
import 'package:flutter/material.dart';
import 'package:front_end/dataBase/dbManager.dart';
import '../modeleDEClasse/transmission.dart';
import 'form_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  // Instance du helper pour accéder à la base de données
  final dbHelper = DBHelper();

  // Instance du service API
  final apiService = ApiService();

  // Liste locale synchronisée avec la base de données
  List<Transmission> transmissions = [];

  @override
  void initState() {
    super.initState();
    _refreshList(); // Charger les données dès l'initialisation de l'écran
  }

  // Fonction pour lire la base de données et mettre à jour l'interface
  void _refreshList() async {
    final data = await dbHelper.getAll();
    setState(() {
      transmissions = data;
    });
  }

  void _afficherDetails(Transmission item) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text("Détails : ${item.nom}"),
        content: SingleChildScrollView(
          child: ListBody(
            children: [
              Text(
                "Provenance : ${item.provenance} (${item.provenanceResult ?? ''})",
              ),
              Text("Type : ${item.type}"),
              if (item.type == "déposition")
                Text("Responsable : ${item.responsable ?? ''}"),
              Text("Objet : ${item.details}"),
              Text("Quantité : ${item.quantite}"),
              const SizedBox(height: 15),
              Text("Commentaire : ${item.details}"),
              const Divider(),
              Text(
                "Statut : ${item.estTerminee ? (item.type == "prêt" ? "Rendu - Terminée" : "Récupéré - Terminée") : "En cours"}",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: item.estTerminee ? Colors.green : Colors.orange,
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Fermer"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("CAHIER DE TRANSMISSION"),
        centerTitle: true,
        backgroundColor: Color(0xFFDC1F3F),
        foregroundColor: Colors.white,
        actions: [
          // Optionnel : bouton pour rafraîchir manuellement
          IconButton(
            icon: const Icon(Icons.cloud_upload),
            onPressed: () async {
              final messenger = ScaffoldMessenger.of(context);
              messenger.showSnackBar(
                const SnackBar(content: Text("Synchronisation en cours...")),
              );

              final syncedCount = await apiService.synchroniserTout();
              _refreshList();

              if (syncedCount > 0) {
                messenger.showSnackBar(
                  SnackBar(
                    content: Text(
                      "$syncedCount transmission(s) synchronisée(s)",
                    ),
                  ),
                );
              } else {
                messenger.showSnackBar(
                  const SnackBar(
                    content: Text(
                      "Aucune donnée synchronisée. Vérifiez que le serveur est lancé.",
                    ),
                  ),
                );
              }
            },
          ),
        ],
      ),
      body: transmissions.isEmpty
          ? const Center(
              child: Text(
                "Aucune transmission n'a été enregistrée",
                style: TextStyle(fontSize: 18, color: Colors.grey),
              ),
            )
          : ListView.builder(
              itemCount: transmissions.length,
              itemBuilder: (context, index) {
                final item = transmissions[index];
                final bool isExterne = item.provenance == 'externe';
                final Color accentColor = isExterne
                    ? Colors.red.shade700
                    : Colors.amber.shade700;
                final Color backgroundColor = Colors.white;

                return Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16.0,
                    vertical: 8.0,
                  ),
                  child: Container(
                    decoration: BoxDecoration(
                      color: backgroundColor,
                      borderRadius: BorderRadius.circular(16.0),
                      border: Border.all(
                        color: Colors.grey.shade200,
                        width: 1.0,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.08),
                          blurRadius: 14,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: ListTile(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 20.0,
                        vertical: 14.0,
                      ),
                      leading: Container(
                        width: 8,
                        height: double.infinity,
                        decoration: BoxDecoration(
                          color: accentColor,
                          borderRadius: BorderRadius.circular(8.0),
                        ),
                      ),
                      title: Text(
                        item.nom,
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: Colors.grey[900],
                        ),
                      ),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            "Le ${item.date.day}/${item.date.month}/${item.date.year}",
                            style: TextStyle(color: Colors.grey[700]),
                          ),
                          const SizedBox(height: 6),
                          Wrap(
                            spacing: 8,
                            runSpacing: 4,
                            children: [
                              Text(
                                item.isSynced
                                    ? "— Sauvegardée"
                                    : "— Hors ligne",
                                style: TextStyle(
                                  color: item.isSynced
                                      ? Colors.green
                                      : Colors.red,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              if (item.estTerminee)
                                Text(
                                  item.type == "prêt"
                                      ? "— Rendu - Terminée"
                                      : "— Récupéré - Terminée",
                                  style: const TextStyle(
                                    color: Colors.green,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                            isExterne
                                ? 'Provenance externe'
                                : 'Provenance interne',
                            style: TextStyle(
                              color: accentColor.withOpacity(0.95),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                      trailing: PopupMenuButton<String>(
                        onSelected: (value) async {
                          if (value == 'delete') {
                            // Suppression en base de données
                            await dbHelper.delete(item.ID!);
                            _refreshList();
                          } else if (value == 'show') {
                            _afficherDetails(item);
                          } else if (value == 'toggle_status') {
                            // Mise à jour du statut en base de données
                            await dbHelper.updateStatus(item.ID!, true);
                            _refreshList();

                            // Essayer de synchroniser avec le serveur si l'enregistrement distant existe
                            if (item.serverId != null) {
                              final success =
                                  await ApiService.updateTransmissionOnServer(
                                    item.serverId!,
                                    {'estTerminee': true, 'is_synced': true},
                                  );
                              if (success) {
                                await dbHelper.markAsSynced(item.ID!);
                                if (!mounted) return;
                                _refreshList();
                              } else {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      "Statut mis à jour localement, synchronisation serveur échouée",
                                    ),
                                  ),
                                );
                              }
                            } else {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text(
                                    "Statut mis à jour localement. Synchronisation serveur non disponible pour cet enregistrement.",
                                  ),
                                ),
                              );
                            }
                          }
                        },
                        itemBuilder: (BuildContext context) => [
                          const PopupMenuItem(
                            value: 'show',
                            child: ListTile(
                              leading: Icon(Icons.visibility),
                              title: Text('Afficher'),
                            ),
                          ),
                          if (!item.estTerminee)
                            PopupMenuItem(
                              value: 'toggle_status',
                              child: ListTile(
                                leading: Icon(
                                  item.type == "prêt"
                                      ? Icons.assignment_return
                                      : Icons.get_app,
                                ),
                                title: Text(
                                  item.type == "prêt" ? "Rendre" : "Récupérer",
                                ),
                              ),
                            ),

                          //delete option
                          const PopupMenuItem(
                            value: 'delete',
                            child: ListTile(
                              leading: Icon(Icons.delete, color: Colors.red),
                              title: Text(
                                'Supprimer',
                                style: TextStyle(color: Colors.red),
                              ),
                            ),
                          ),
                        ],
                        icon: const Icon(Icons.more_vert),
                      ),
                    ),
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final Transmission? nouvelleTransmission = await Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const FormScreen()),
          );

          if (nouvelleTransmission != null) {
            // 1. On insère en local d'abord (toujours)
            int localId = await dbHelper.insert(nouvelleTransmission);

            // On met à jour l'objet avec l'ID généré par SQLite pour que le serveur le reçoive
            nouvelleTransmission.ID = localId;

            // On affiche immédiatement la nouvelle transmission depuis SQLite
            _refreshList();

            // 2. Envoi au serveur en tâche de fond sans bloquer l'affichage
            final success = await ApiService.envoyerAuServeur(
              nouvelleTransmission,
            );
            if (success) {
              await dbHelper.markAsSynced(localId);
              if (!mounted) return;
              _refreshList();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text("Synchronisé avec le serveur !")),
              );
            } else {
              if (!mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text("Enregistré en local (Serveur hors-ligne)"),
                ),
              );
            }
          }
        },
        backgroundColor: const Color(0xFFD49A00),
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }
}
