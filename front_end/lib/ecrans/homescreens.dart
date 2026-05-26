// ignore_for_file: file_names

import 'package:flutter/material.dart';
import 'package:front_end/services/transmission_repository.dart';
import 'package:front_end/widgets/transmission_filter_dialog.dart';
import '../modeleDEClasse/transmission.dart';
import 'form_screen.dart';
import '../widgets/_afficherDetails.dart';
import '../widgets/transmission_scaffold.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final TransmissionRepository repository = TransmissionRepository();
  List<Transmission> transmissions = [];
  List<Transmission> filteredTransmissions = [];
  bool isSyncing = false;
  
  // Variables pour stocker les filtres actifs
  DateTime? filterDate;
  String? filterType;
  String? filterCategory;

  @override
  void initState() {
    super.initState();
    _refreshList(); // Charger les données dès l'initialisation de l'écran
  }

  Future<void> _refreshList() async {
    final data = await repository.fetchAll();
    if (!mounted) return;
    setState(() {
      transmissions = data;
      _applyFilters();
    });
  }

  /// Applique les filtres actuels aux transmissions
  void _applyFilters() {
    filteredTransmissions = TransmissionFilterDialog.filterTransmissions(
      transmissions,
      filterDate: filterDate,
      filterType: filterType,
      filterCategory: filterCategory,
    );
  }

  /// Ouvre le dialogue de filtrage
  Future<void> _openFilterDialog() async {
    final result = await TransmissionFilterDialog(context: context).show(
      filterDate,
      filterType,
      filterCategory,
    );

    if (result != null) {
      setState(() {
        filterDate = result['date'];
        filterType = result['type'];
        filterCategory = result['category'];
        _applyFilters();
      });
    }
  }

  void _afficherDetails(Transmission item) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (context) => TransmissionDetailsDialog(item: item),
    );
  }

  Future<void> _syncAll() async {
    if (!mounted) return;
    setState(() => isSyncing = true);

    final syncedCount = await repository.syncAll();
    await _refreshList();

    if (!mounted) return;
    setState(() => isSyncing = false);

    String snackBarMessage;
    if (transmissions.isEmpty) {
      snackBarMessage = "Aucune donnée à synchroniser.";
    } else if (syncedCount > 0) {
      snackBarMessage = "$syncedCount transmission(s) synchronisée(s)";
    } else {
      snackBarMessage = "Toutes les données sont déjà synchronisées.";
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(snackBarMessage),
      ),
    );
  }

  Future<void> _deleteTransmission(Transmission item) async {
    if (item.ID == null) return;
    await repository.deleteById(item.ID!);
    await _refreshList();
  }

  Future<void> _toggleTransmissionStatus(Transmission item) async {
    // Afficher un dialogue permettant de choisir l'etat et saisir une remarque
    String selectedEtat = item.etat ?? 'suivi';
    final TextEditingController remarqueController = TextEditingController(text: item.remarque ?? '');

    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: true,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: Text(item.type == 'prêt' ? 'Rendre' : 'Récupérer'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Dropdown
                  DropdownButtonFormField<String>(
                    initialValue: selectedEtat,
                    items: const [
                      DropdownMenuItem(value: 'terminer', child: Text('TERMINER')),
                      DropdownMenuItem(value: 'suivi', child: Text('SUIVI')),
                    ],
                    onChanged: (v) {
                      if (v != null) setState(() => selectedEtat = v);
                    },
                    decoration: const InputDecoration(labelText: 'Etat'),
                  ),
                  const SizedBox(height: 12),
                  // Remark textfield
                  TextField(
                    controller: remarqueController,
                    maxLength: 75,
                    decoration: const InputDecoration(labelText: 'Remarque'),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(false),
                  child: const Text('Annuler'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    // Validate length
                    if (remarqueController.text.length > 75) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Remarque trop longue (75 caractères max)')));
                      return;
                    }

                    // Enregistrer localement et essayer de synchroniser
                    final success = await repository.finalizeTransmission(item, selectedEtat, remarqueController.text.trim().isEmpty ? null : remarqueController.text.trim());
                    await _refreshList();
                    if (!mounted) return;
                    if (success) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('État et remarque enregistrés.')));
                    } else {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enregistré localement, synchronisation échouée.')));
                    }
                    Navigator.of(context).pop(true);
                  },
                  child: const Text('Terminé'),
                ),
              ],
            );
          },
        );
      },
    );

    if (result == true) {
      // refresh effectué après enregistrement
    }
  }

  Future<void> _ajouterTransmission() async {
    final Transmission? nouvelleTransmission = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const FormScreen()),
    );

    if (nouvelleTransmission == null) {
      return;
    }

    final bool success = await repository.saveAndSync(nouvelleTransmission);
    await _refreshList();

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          success
              ? "Synchronisé avec le serveur !"
              : "Enregistré en local (Serveur hors-ligne)",
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {

    return TransmissionScaffold(
      transmissions: filteredTransmissions,
      onSync: _syncAll,
      onAdd: _ajouterTransmission,
      onFilter: _openFilterDialog,
      onDelete: _deleteTransmission,
      onShowDetails: _afficherDetails,
      onToggleStatus: _toggleTransmissionStatus,
    );
  }
}
