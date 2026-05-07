// ignore_for_file: non_constant_identifier_names, camel_case_types

class Transmission {
  int? ID;
  final String nom;
  final String provenance;
  final String? provenanceResult;
  final String type;
  final String? responsable;
  final int quantite;
  final String details;
  final DateTime date;
  bool estTerminee;
  bool isSynced;

  // constructeur de @transmission
  Transmission({
    this.ID,
    required this.nom,
    required this.provenance,
    this.provenanceResult,
    required this.type,
    this.responsable,
    required this.quantite,
    required this.details,
    required this.date,
    this.estTerminee = false,
    this.isSynced = false,
  });
  // ce methode convertit an Transmission object into Map pour l'inserer dans la base
  Map<String, dynamic> toMap() {
    return {
      if (ID != null) 'id': ID,
      'nom': nom,
      'provenance': provenance,
      'provenanceResult': provenanceResult,
      'type': type,
      'responsable': responsable,
      'quantite': quantite,
      'details': details,
      'date': date.toIso8601String(),
      'estTerminee': estTerminee,
      'is_synced': isSynced ? 1 : 0,
    };
  }

  // ce methode est l'inverse de toMap() defini en haut donc de sqlite vers un instance de Transmission
  factory Transmission.fromMap(Map<String, dynamic> map) {
    return Transmission(
      ID: map['id'] ?? map['ID'],
      nom: map['nom'],
      provenance: map['provenance'],
      provenanceResult: map['provenanceResult'],
      type: map['type'],
      responsable: map['responsable'],
      quantite: map['quantite'],
      details: map['details'],
      date: DateTime.parse(map['date']),
      estTerminee: map['estTerminee'] == 1 || map['estTerminee'] == true,
      isSynced: map['is_synced'] == 1 || map['is_synced'] == true,
    );
  }
}
