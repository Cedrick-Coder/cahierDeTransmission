import '../dataBase/dbManager.dart';
import '../modeleDEClasse/transmission.dart';
import 'api_services.dart';

class TransmissionRepository {
  final DBHelper dbHelper = DBHelper();
  final ApiService apiService = ApiService();

  Future<List<Transmission>> fetchAll() => dbHelper.getAll();

  Future<int> syncAll() => apiService.synchroniserTout();

  Future<int> deleteById(int id) => dbHelper.delete(id);

  Future<bool> saveAndSync(Transmission transmission) async {
    final localId = await dbHelper.insert(transmission);
    transmission.ID = localId;
    final success = await ApiService.envoyerAuServeur(transmission);
    if (success) {
      await dbHelper.markAsSynced(localId);
    }
    return success;
  }

  Future<bool> toggleStatus(Transmission item) async {
    if (item.ID == null) return false;

    await dbHelper.updateStatus(item.ID!, true);

    if (item.serverId == null) {
      return false;
    }

    final success = await ApiService.updateTransmissionOnServer(
      item.serverId!,
      {'estTerminee': true, 'is_synced': true},
    );

    if (success) {
      await dbHelper.markAsSynced(item.ID!);
    }

    return success;
  }
}
