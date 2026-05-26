import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../modeleDEClasse/transmission.dart';
import '../dataBase/dbManager.dart';

class ApiService {
  // 10.0.2.2 == adresse pour accéder au localhost depuis l'émulateur Android
  static const String baseUrl = "http://192.168.0.150:8000/api";

  static final DBHelper dbHelper = DBHelper();

  /// Envoie un seul record au serveur (utilisé lors de la création)
  static Future<bool> envoyerAuServeur(Transmission transmission) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/sync'),
            headers: {'Content-Type': 'application/json'},
            body: json.encode(transmission.toJson()),
          )
          .timeout(const Duration(seconds: 30)); // Augmenté à 30 secondes
      
      if (response.statusCode == 201 || response.statusCode == 200) {
        try {
          final body = json.decode(response.body);
          int? serverId;

          if (body is Map<String, dynamic>) {
            final data = body['data'];
            if (data is Map<String, dynamic> && data['id'] != null) {
              serverId = data['id'] is int ? data['id'] : int.tryParse(data['id'].toString());
            } else if (body['id'] != null) {
              serverId = body['id'] is int ? body['id'] : int.tryParse(body['id'].toString());
            }
          }

          if (serverId != null && transmission.ID != null) {
            await dbHelper.updateServerId(transmission.ID!, serverId);
            transmission.serverId = serverId;
          }
        } catch (_) {
          // Si le serveur répond correctement mais sans JSON valide,
          // on considère que l'envoi a réussi.
        }

        return true;
      } else {
        return false;
      }
    } on TimeoutException {
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
      return 0;
    }

    int syncedCount = 0;
    for (var transmission in unsyncedData) {
      bool success;
      if (transmission.serverId != null) {
        success = await updateTransmissionOnServer(
          transmission.serverId!,
          {
            'estTerminee': transmission.estTerminee,
            'etat': transmission.etat,
            'remarque': transmission.remarque,
            'is_synced': true,
          },
        );
      } else {
        success = await envoyerAuServeur(transmission);
      }

      if (success) {
        await dbHelper.markAsSynced(transmission.ID!);
        syncedCount++;
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
      }
    } catch (e) {
      print("Erreur réseau : $e");
    }
  }

  /// Met à jour une transmission sur le serveur
  static Future<bool> updateTransmissionOnServer(int id, Map<String, dynamic> updates) async {
    try {
      final response = await http
          .put(
            Uri.parse('$baseUrl/transmissions/$id'),
            headers: {'Content-Type': 'application/json'},
            body: json.encode(updates),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return true;
      } else {
        return false;
      }
    } on TimeoutException {
      return false;
    } catch (e) {
      return false;
    }
  }
}
