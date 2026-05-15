// ignore_for_file: non_constant_identifier_names, camel_case_types

class Transmission {
  int? ID;
  int? serverId;
  final String nom;
  final String provenance;
  final String? provenanceResult;
  final String type;
  final String? responsable;
  final int quantite;
  final String details;
  final DateTime date;
  final DateTime? dateRemise;
  bool estTerminee;
  bool isSynced;

  // constructeur de @transmission
  Transmission({
    this.ID,
    this.serverId,
    required this.nom,
    required this.provenance,
    this.provenanceResult,
    required this.type,
    this.responsable,
    required this.quantite,
    required this.details,
    required this.date,
    this.dateRemise,
    this.estTerminee = false,
    this.isSynced = false,
  });

  // ce methode convertit un Transmission object en Map pour l'inserer dans la base locale
  Map<String, dynamic> toMap() {
    return {
      if (ID != null) 'id': ID,
      if (serverId != null) 'server_id': serverId,
      'nom': nom,
      'provenance': provenance,
      'provenanceResult': provenanceResult,
      'type': type,
      'responsable': responsable,
      'quantite': quantite,
      'details': details,
      'date': date.toIso8601String(),
      'date_remise': dateRemise?.toIso8601String(),
      'estTerminee': estTerminee,
      'is_synced': isSynced ? 1 : 0,
    };
  }

  // ce methode convertit un Transmission object en Map JSON pour envoyer au serveur
  Map<String, dynamic> toJson() {
    return {
      'nom': nom,
      'provenance': provenance,
      'provenanceResult': provenanceResult,
      'type': type,
      'responsable': responsable,
      'quantite': quantite,
      'details': details,
      'date': date.toIso8601String(),
      'date_remise': dateRemise?.toIso8601String(),
      'estTerminee': estTerminee,
      'is_synced': isSynced,
    };
  }

  // ce methode est l'inverse de toMap() defini en haut donc de sqlite vers un instance de Transmission
  factory Transmission.fromMap(Map<String, dynamic> map) {
    return Transmission(
      ID: map['id'] ?? map['ID'],
      serverId: map['server_id'] ?? map['serverId'],
      nom: map['nom'],
      provenance: map['provenance'],
      provenanceResult: map['provenanceResult'],
      type: map['type'],
      responsable: map['responsable'],
      quantite: map['quantite'],
      details: map['details'],
      date: DateTime.parse(map['date']),
      dateRemise: map['date_remise'] != null
          ? DateTime.parse(map['date_remise'])
          : null,
      estTerminee: map['estTerminee'] == 1 || map['estTerminee'] == true,
      isSynced: map['is_synced'] == 1 || map['is_synced'] == true,
    );
  }
}
