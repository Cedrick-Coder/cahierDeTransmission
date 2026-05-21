// ignore_for_file: deprecated_member_use

import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';
import 'package:intl/intl.dart';
import 'package:front_end/constData/directionEtCaisse.dart';
import '../widgets/form_sections.dart';

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
  final TextEditingController _dateDelivranceController = TextEditingController(
    text: "00-00-0000",
  );

  // Variables d'état
  String provenance = "interne";
  String type = "prêt";
  final DateTime _dateFixe = DateTime.now();

  //variables pour les listes déroulantes
  String? selectedDirection;
  String? selectedAgence;
  String? selectedCaisse;
  List<String> caissesDisponibles = [];

  // appel des données pour direction
  final List<String> directions = directionEtCaisse.directions;

  // appel des données pour agence et caisse
  final Map<String, List<String>> agencesEtCaisses = directionEtCaisse.agencesEtCaisses;

  @override
  void dispose() {
    _nomController.dispose();
    _quantiteController.dispose();
    _detailsController.dispose();
    _subProvenanceController.dispose();
    _responsableController.dispose();
    _dateDelivranceController.dispose();
    super.dispose();
  }

  Future<void> _pickDate(BuildContext context) async {
    final DateTime now = DateTime.now();
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: now.add(const Duration(days: 1)),
      firstDate: DateTime.now(),
      lastDate: DateTime(now.year + 10),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.light(
              primary: Colors.deepPurple.shade400,
              onPrimary: Colors.white,
              onSurface: Colors.black,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        _dateDelivranceController.text = DateFormat(
          'dd-MM-yyyy',
        ).format(picked);
      });
    }
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

              ProvenanceSelector(
                provenance: provenance,
                onChanged: (value) {
                  setState(() {
                    provenance = value.toString();
                    _subProvenanceController.clear();
                    selectedDirection = null;
                    selectedAgence = null;
                    selectedCaisse = null;
                    caissesDisponibles = [];
                  });
                },
              ),

              if (provenance == "interne")
                DirectionDropdown(
                  directions: directions,
                  selectedDirection: selectedDirection,
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
                AgenceCaisseSelector(
                  agences: agencesEtCaisses.keys.toList(),
                  selectedAgence: selectedAgence,
                  selectedCaisse: selectedCaisse,
                  caissesDisponibles: caissesDisponibles,
                  onAgenceChanged: (val) {
                    setState(() {
                      selectedAgence = val;
                      selectedCaisse = null;
                      caissesDisponibles = val != null
                          ? agencesEtCaisses[val]!
                          : [];
                    });
                  },
                  onCaisseChanged: (val) {
                    setState(() {
                      selectedCaisse = val;
                      _subProvenanceController.text =
                          "$selectedAgence - $val";
                    });
                  },
                  agenceValidator: (value) =>
                      value == null ? "Agence ?" : null,
                  caisseValidator: (value) =>
                      value == null ? "Caisse ?" : null,
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

              TypeSelector(
                type: type,
                onChanged: (v) {
                  setState(() {
                    type = v.toString();
                    if (type == "déposition") {
                      _dateDelivranceController.text = "00-00-0000";
                    }
                  });
                },
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

              if (type == "prêt")
                DateRemiseField(
                  controller: _dateDelivranceController,
                  onPickDate: () => _pickDate(context),
                  onClearDate: () {
                    setState(() {
                      _dateDelivranceController.text = "00-00-0000";
                    });
                  },
                  validator: (value) {
                    if (value == null ||
                        value.isEmpty ||
                        value == "00-00-0000") {
                      return 'Veuillez sélectionner une date de remise';
                    }
                    try {
                      DateFormat('dd-MM-yyyy').parseStrict(value);
                    } catch (_) {
                      return 'Date invalide';
                    }
                    return null;
                  },
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
                      final DateTime selectedRemiseDate = type == "prêt"
                          ? DateFormat(
                              'dd-MM-yyyy',
                            ).parseStrict(_dateDelivranceController.text)
                          : _dateFixe;
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
                        dateRemise: type == "prêt" ? selectedRemiseDate : null,
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
