import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../modeleDEClasse/transmission.dart';
import '../dataBase/dbManager.dart';

class ApiService {
  // 10.0.2.2 est l'adresse pour accéder au localhost depuis l'émulateur Android
  static const String baseUrl = "http://192.168.0.150:8000/api";

  final dbHelper = DBHelper();

  /// Envoie un seul record au serveur (utilisé lors de la création)
  static Future<bool> envoyerAuServeur(Transmission transmission) async {
    try {
      transmission.toMap();
      final response = await http
          .post(
            Uri.parse('$baseUrl/sync'),
            headers: {'Content-Type': 'application/json'},
            body: json.encode(transmission.toMap()),
          )
          .timeout(const Duration(seconds: 30)); // Augmenté à 30 secondes
      
      if (response.statusCode == 201 || response.statusCode == 200) {
        return true;
      } else {
        return false;
      }
    } on TimeoutException catch (e) {
      return false;
    } catch (e) {
      return false;
    }
  }

  /// Récupère tous les records non synchronisés en local et les envoie en boucle
  Future<int> synchroniserTout() async {
    // Récupérer les données locales où is_synced = 0
    List<Transmission> unsyncedData = await dbHelper.getUnsynced();

    if (unsyncedData.isEmpty) {
      print("Toutes les données sont déjà à jour.");
      return 0;
    }

    int syncedCount = 0;
    for (var transmission in unsyncedData) {
      bool success = await envoyerAuServeur(transmission);
      if (success) {
        await dbHelper.markAsSynced(transmission.ID!);
        syncedCount++;
        print("Record ${transmission.ID} marqué comme synchronisé en local.");
      }
    }

    return syncedCount;
  }

  /// Récupère les données du serveur et les insère en local
  Future<void> syncDataFromServer() async {
    try {
      final response = await http.get(Uri.parse('$baseUrl/transmissions'));

      if (response.statusCode == 200) {
        List<dynamic> data = json.decode(response.body);
        List<Transmission> transmissions = data
            .map((item) => Transmission.fromMap(item))
            .toList();

        for (var transmission in transmissions) {
          // Insérer ou mettre à jour en local
          await dbHelper.insert(transmission);
        }
        print("Synchronisation depuis le serveur réussie");
      } else {
        print("Erreur serveur : ${response.body}");
      }
    } catch (e) {
      print("Erreur réseau : $e");
    }
  }
}
