// ignore_for_file: file_names

import 'package:flutter/material.dart';
import 'package:front_end/services/transmission_repository.dart';
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
  bool isSyncing = false;

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
    });
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

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          syncedCount > 0
              ? "$syncedCount transmission(s) synchronisée(s)"
              : "Aucune donnée synchronisée. Vérifiez que le serveur est lancé.",
        ),
      ),
    );
  }

  Future<void> _deleteTransmission(Transmission item) async {
    if (item.ID == null) return;
    await repository.deleteById(item.ID!);
    await _refreshList();
  }

  Future<void> _toggleTransmissionStatus(Transmission item) async {
    final bool success = await repository.toggleStatus(item);
    await _refreshList();

    if (!mounted) return;
    if (item.serverId == null && !success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            "Statut mis à jour localement. Synchronisation serveur non disponible pour cet enregistrement.",
          ),
        ),
      );
      return;
    }

    if (!success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            "Statut mis à jour localement, synchronisation serveur échouée",
          ),
        ),
      );
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
      transmissions: transmissions,
      onSync: _syncAll,
      onAdd: _ajouterTransmission,
      onDelete: _deleteTransmission,
      onShowDetails: _afficherDetails,
      onToggleStatus: _toggleTransmissionStatus,
    );
  }
}
