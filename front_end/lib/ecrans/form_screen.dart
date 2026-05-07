// ignore_for_file: deprecated_member_use

import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';

class FormScreen extends StatefulWidget {
  const FormScreen({super.key});

  @override
  State<FormScreen> createState() => _FormScreenState();
}

class _FormScreenState extends State<FormScreen> {
  final _formKey = GlobalKey<FormState>();

  // Contrôleurs
  final TextEditingController _nomController = TextEditingController();
  final TextEditingController _objetController = TextEditingController();
  final TextEditingController _quantiteController = TextEditingController();
  final TextEditingController _detailsController = TextEditingController();
  final TextEditingController _subProvenanceController =
      TextEditingController();
  final TextEditingController _responsableController = TextEditingController();

  // Variables d'état
  String provenance = "interne";
  String type = "prêt";
  final DateTime _dateFixe = DateTime.now();

  //variables pour les listes déroulantes
  String? selectedDirection;
  String? selectedAgence;
  String? selectedCaisse;
  List<String> caissesDisponibles = [];

  // Données pour le choix Interne
  final List<String> directions = [
    "Direction Générale",
    "Direction des Ressources Humaines",
    "Direction Financière",
    "Direction Technique",
    "Direction Commerciale",
    "Direction Logistique",
    "Direction Comptabilité",
    "Direction des Declarations",
    "Direction Juriste",
    "Direction Audit",
    "Direction Marketing",
    "Direction des Opération",
    "Direction du Mutuelle de Santé",
    "Direction du Responsable Controle Permanent",
  ];

  // agence/caisse en map(data struct = map/dictionnaire)
  final Map<String, List<String>> agencesEtCaisses = {
    "Farimbontsoa": [
      "67ha",
      "Antohomadinika",
      "Ampefiloha",
      "Anosibe",
      "Ankazomanga",
      "Anosizato",
    ],
    "Aina": [
      "Ambanidia",
      "Ambohipo",
      "Mandroseza",
      "Andrefana",
      "Soanierana",
      "Andramasina",
      "Arivonimamo",
      "Miarinarivo",
      "Soavinandriana",
      "Tsiroanomandidy",
    ],
    "Vonjy": [
      "Mahavoky",
      "Andravoahangy",
      "Ankadidramamy",
      "Analamahitsy",
      "Alarobia",
      "Soarano",
      "Avaradoha",
    ],
    "Rindra": [
      "Mangamila",
      "Ankazondandy",
      "Ambatomena",
      "Manjakandrina",
      "Anjepy",
      "Anjozorobe",
    ],
    "Mahasoa": [
      "Ambohibao",
      "Ambohidratrimo",
      "Ivato",
      "Ambohimanarina",
      "Ambatolampy Tsimahafotsy",
    ],
    "Fanavotana": [
      "Itaosy",
      "Alasora",
      "Andoharanofotsy",
      "Ampitatafika",
      "Imeritsiatosika",
      "Tanjombato",
    ],
    "Fanantenana": ["Ambositra", "Fandriana", "Fianarantsoa", "Ambalavao"],
    "Manoro": ["Faratsiho", "Betafo", "Antsenakely", "Ambatolampy", "Behenjy"],
    "Majunga": ["Marovoay", "Ambato-boeny", "Manjarisoa", "Tanambao sotema"],
    "Betsiboka": ["Maevatanana", "Andriba", "Manerinerina"],
  };

  @override
  void dispose() {
    _nomController.dispose();
    _quantiteController.dispose();
    _detailsController.dispose();
    _subProvenanceController.dispose();
    _responsableController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Nouvelle Transmission"),
        centerTitle: true,
        backgroundColor: Color(0xFFDC1F3F),
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // champ nom
              TextFormField(
                controller: _nomController,
                decoration: const InputDecoration(
                  labelText: "Nom",
                  border: OutlineInputBorder(),
                ),
                validator: (value) => (value == null || value.isEmpty)
                    ? "Ce champ est obligatoire"
                    : null,
              ),
              const SizedBox(height: 20),

              // Choix Provenance
              const Text(
                "Provenance :",
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Row(
                children: [
                  Radio(
                    value: "interne",
                    groupValue: provenance,
                    onChanged: (value) {
                      setState(() {
                        provenance = value.toString();
                        _subProvenanceController.clear();
                        selectedDirection = null;
                      });
                    },
                  ),
                  const Text("Interne"),
                  Radio(
                    value: "externe",
                    groupValue: provenance,
                    onChanged: (value) {
                      setState(() {
                        provenance = value.toString();
                        _subProvenanceController.clear();
                        selectedAgence = null;
                        selectedCaisse = null;
                      });
                    },
                  ),
                  const Text("Externe"),
                ],
              ),

              //logique de la liste déroulante
              if (provenance == "interne")
                DropdownButtonFormField<String>(
                  value: selectedDirection,
                  decoration: const InputDecoration(
                    labelText: "Sélectionner la Direction",
                    border: OutlineInputBorder(),
                  ),
                  items: directions
                      .map(
                        (dir) => DropdownMenuItem(value: dir, child: Text(dir)),
                      )
                      .toList(),
                  onChanged: (val) {
                    setState(() {
                      selectedDirection = val;
                      _subProvenanceController.text = val!;
                    });
                  },
                  validator: (value) =>
                      value == null ? "Sélectionnez une direction" : null,
                )
              else
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: selectedAgence,
                        decoration: const InputDecoration(
                          labelText: "Agence",
                          border: OutlineInputBorder(),
                        ),
                        items: agencesEtCaisses.keys
                            .map(
                              (ag) => DropdownMenuItem(
                                value: ag,
                                child: Text(
                                  ag,
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (val) {
                          setState(() {
                            selectedAgence = val;
                            selectedCaisse = null;
                            caissesDisponibles = agencesEtCaisses[val]!;
                          });
                        },
                        validator: (value) => value == null ? "Agence ?" : null,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: selectedCaisse,
                        decoration: const InputDecoration(
                          labelText: "Caisse",
                          border: OutlineInputBorder(),
                        ),
                        items: caissesDisponibles
                            .map(
                              (c) => DropdownMenuItem(
                                value: c,
                                child: Text(
                                  c,
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (val) {
                          setState(() {
                            selectedCaisse = val;
                            _subProvenanceController.text =
                                "$selectedAgence - $val";
                          });
                        },
                        validator: (value) => value == null ? "Caisse ?" : null,
                      ),
                    ),
                  ],
                ),

              const SizedBox(height: 20),

              // Champ Nom de l'objet
              TextFormField(
                controller: _objetController,
                decoration: const InputDecoration(
                  labelText: "Nom de l'objet",
                  border: OutlineInputBorder(),
                ),
                validator: (value) =>
                    value!.isEmpty ? "Champ obligatoire" : null,
              ),
              const SizedBox(height: 20),

              // Choix Type
              const Text(
                "Type de mouvement :",
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Row(
                children: [
                  Radio(
                    value: "prêt",
                    groupValue: type,
                    onChanged: (v) => setState(() => type = v.toString()),
                  ),
                  const Text("Prêt"),
                  Radio(
                    value: "déposition",
                    groupValue: type,
                    onChanged: (v) => setState(() => type = v.toString()),
                  ),
                  const Text("Déposition"),
                ],
              ),

              if (type == "déposition")
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: TextFormField(
                    controller: _responsableController,
                    decoration: const InputDecoration(
                      labelText: "Responsable de déposition",
                      border: OutlineInputBorder(),
                    ),
                    validator: (value) =>
                        value!.isEmpty ? "Précisez le responsable" : null,
                  ),
                ),

              const SizedBox(height: 20),

              // Quantité et Détails
              TextFormField(
                controller: _quantiteController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: "Quantité",
                  border: OutlineInputBorder(),
                ),
                validator: (value) => value!.isEmpty ? "Obligatoire" : null,
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _detailsController,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: "Détails / Observations",
                  border: OutlineInputBorder(),
                ),
              ),

              const SizedBox(height: 30),

              // Bouton Valider
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: () {
                    if (_formKey.currentState!.validate()) {
                      final nouvelleTransmission = Transmission(
                        nom: _nomController.text,
                        provenance: provenance,
                        provenanceResult: _subProvenanceController.text,
                        type: type,
                        responsable: type == "déposition"
                            ? _responsableController.text
                            : null,
                        quantite: int.parse(_quantiteController.text),
                        details: _detailsController.text,
                        date: _dateFixe,
                      );
                      Navigator.pop(context, nouvelleTransmission);
                    }
                  },
                  child: const Text("TERMINÉ"),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
