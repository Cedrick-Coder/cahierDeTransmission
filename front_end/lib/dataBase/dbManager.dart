// ignore_for_file: file_names

import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../modeleDEClasse/transmission.dart';

class DBHelper {
  static Database? _db;

  Future<Database> get db async {
    if (_db != null) return _db!;
    _db = await initDb();
    return _db!;
  }

  Future<Database> initDb() async {
    String path = join(await getDatabasesPath(), "transmission.db");
    return await openDatabase(
      path,
      version: 4,
      onCreate: (db, version) async {
        await db.execute('''
        CREATE TABLE transmissions (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          server_id INTEGER,
          nom TEXT,
          provenance TEXT,
          provenanceResult TEXT,
          type TEXT,
          responsable TEXT,
          quantite INTEGER,
          details TEXT,
          date TEXT,
          date_remise TEXT,
          estTerminee INTEGER,
          etat TEXT,
          remarque TEXT,
          is_synced INTEGER DEFAULT 0
        )
      ''');
      },
      // MAJ la table si existe
      onUpgrade: (db, oldVersion, newVersion) async {
        if (oldVersion < 2) {
          await db.execute(
            "ALTER TABLE transmissions ADD COLUMN server_id INTEGER",
          );
          await db.execute(
            "ALTER TABLE transmissions ADD COLUMN is_synced INTEGER DEFAULT 0",
          );
        }
        if (oldVersion < 3) {
          await db.execute(
            "ALTER TABLE transmissions ADD COLUMN date_remise TEXT",
          );
        }
        if (oldVersion < 4) {
          await db.execute(
            "ALTER TABLE transmissions ADD COLUMN etat TEXT",
          );
          await db.execute(
            "ALTER TABLE transmissions ADD COLUMN remarque TEXT",
          );
        }
      },
    );
  }

  Future<int> insert(Transmission t) async {
    // Insertion d'une transmission dans la table
    var dbClient = await db;
    Map<String, dynamic> data = t.toMap();
    data['estTerminee'] = t.estTerminee ? 1 : 0;
    data['etat'] = t.etat ?? 'suivi';
    data['remarque'] = t.remarque;
    data['is_synced'] = t.isSynced ? 1 : 0;
    return await dbClient.insert("transmissions", data);
  }

  // recup data non synchronisé
  Future<List<Transmission>> getUnsynced() async {
    var dbClient = await db;
    List<Map<String, dynamic>> maps = await dbClient.query(
      "transmissions",
      where: "is_synced = ?",
      whereArgs: [0],
    );
    return List.generate(maps.length, (i) => Transmission.fromMap(maps[i]));
  }

  // marquer un transmission as synced
  Future<int> markAsSynced(int id) async {
    var dbClient = await db;
    return await dbClient.update(
      "transmissions",
      {'is_synced': 1},
      where: "id = ?",
      whereArgs: [id],
    );
  }

  // Enregistrer l'identifiant distant renvoyé par le serveur
  Future<int> updateServerId(int localId, int serverId) async {
    var dbClient = await db;
    return await dbClient.update(
      "transmissions",
      {'server_id': serverId},
      where: "id = ?",
      whereArgs: [localId],
    );
  }

  // Récupérer toutes les transmissions
  Future<List<Transmission>> getAll() async {
    var dbClient = await db;
    List<Map<String, dynamic>> maps = await dbClient.query(
      "transmissions",
      orderBy: "id DESC",
    );
    return maps.map((row) => Transmission.fromMap(row)).toList();
  }

  // Mettre à jour le statut (Terminée)
  Future<int> updateStatus(int id, bool status) async {
    var dbClient = await db;
    return await dbClient.update(
      "transmissions",
      {'estTerminee': status ? 1 : 0, 'is_synced': 0},
      where: "id = ?",
      whereArgs: [id],
    );
  }

  // Mettre à jour l'etat et la remarque
  Future<int> updateEtatRemarque(int id, String etat, String? remarque) async {
    var dbClient = await db;
    final int estTermineeVal = (etat == 'terminer') ? 1 : 0;
    return await dbClient.update(
      "transmissions",
      {'etat': etat, 'remarque': remarque, 'estTerminee': estTermineeVal, 'is_synced': 0},
      where: "id = ?",
      whereArgs: [id],
    );
  }

  // Supprimer
  Future<int> delete(int id) async {
    var dbClient = await db;
    return await dbClient.delete(
      "transmissions",
      where: "id = ?",
      whereArgs: [id],
    );
  }
}
