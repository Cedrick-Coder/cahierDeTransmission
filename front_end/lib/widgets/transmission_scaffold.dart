import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';
import 'carteDeTransmission.dart'; 

class TransmissionScaffold extends StatelessWidget {
  final List<Transmission> transmissions;
  final VoidCallback onSync;
  final VoidCallback onAdd;
  final VoidCallback onFilter;
  final Function(Transmission) onDelete;
  final Function(Transmission) onShowDetails;
  final Function(Transmission) onToggleStatus;

  const TransmissionScaffold({
    super.key,
    required this.transmissions,
    required this.onSync,
    required this.onAdd,
    required this.onFilter,
    required this.onDelete,
    required this.onShowDetails,
    required this.onToggleStatus,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("CAHIER DE TRANSMISSION"),
        centerTitle: true,
        backgroundColor: const Color(0xFFDC1F3F),
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.tune),
            onPressed: onFilter,
            tooltip: 'Filtrer',
          ),
          IconButton(
            icon: const Icon(Icons.cloud_upload),
            onPressed: onSync,
            tooltip: 'Synchroniser',
          ),
        ],
      ),
      body: transmissions.isEmpty
          ? const Center(
              child: Text(
                "Aucune transmission n'a été enregistrée",
                style: TextStyle(fontSize: 18, color: Colors.grey),
              ),
            )
          : ListView.builder(
              itemCount: transmissions.length,
              itemBuilder: (context, index) {
                final item = transmissions[index];
                return TransmissionCard(
                  item: item,
                  onDelete: () => onDelete(item),
                  onShowDetails: () => onShowDetails(item),
                  onToggleStatus: () => onToggleStatus(item),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: onAdd,
        backgroundColor: const Color(0xFFD49A00),
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }
}