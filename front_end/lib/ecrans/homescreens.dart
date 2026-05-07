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
                return ListTile(
                  leading: const Icon(Icons.send_time_extension),
                  title: Text(item.nom),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "Le ${item.date.day}/${item.date.month}/${item.date.year}",
                      ),
                      const SizedBox(height: 4),
                      Wrap(
                        spacing: 8,
                        runSpacing: 4,
                        children: [
                          Text(
                            item.isSynced ? "— Sauvegardée" : "— Hors ligne",
                            style: TextStyle(
                              color: item.isSynced ? Colors.green : Colors.red,
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
