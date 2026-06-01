import 'package:flutter/material.dart';
import '../modeleDEClasse/transmission.dart';

class TransmissionFilterDialog {
  final BuildContext context;
  DateTime? selectedDate;
  String? selectedType;
  String? selectedCategory;

  TransmissionFilterDialog({required this.context});

  /// Affiche le dialogue de filtrage et retourne les filtres appliqués
  Future<Map<String, dynamic>?> show(
    DateTime? initialDate,
    String? initialType,
    String? initialCategory,
  ) async {
    selectedDate = initialDate;
    selectedType = initialType;
    selectedCategory = initialCategory;

    return await showDialog<Map<String, dynamic>?>(
      context: context,
      barrierDismissible: true,
      builder: (context) => _FilterDialogContent(
        onDateChanged: (date) => selectedDate = date,
        onTypeChanged: (type) => selectedType = type,
        onCategoryChanged: (category) => selectedCategory = category,
        initialDate: selectedDate,
        initialType: selectedType,
        initialCategory: selectedCategory,
      ),
    );
  }

  /// Filtre une liste de transmissions selon les critères
  static List<Transmission> filterTransmissions(
    List<Transmission> transmissions, {
    DateTime? filterDate,
    String? filterType,
    String? filterCategory,
  }) {
    return transmissions.where((transmission) {
      // Filtre par date (comparaison du jour)
      if (filterDate != null) {
        final transmissionDate = DateTime(
          transmission.date.year,
          transmission.date.month,
          transmission.date.day,
        );
        final filterDateOnly = DateTime(
          filterDate.year,
          filterDate.month,
          filterDate.day,
        );
        if (transmissionDate != filterDateOnly) return false;
      }

      // Filtre par type (interne/externe)
      if (filterType != null && transmission.provenance != filterType) {
        return false;
      }

      // Filtre par catégorie (prêt/déposition)
      if (filterCategory != null && transmission.type != filterCategory) {
        return false;
      }

      return true;
    }).toList();
  }
}

class _FilterDialogContent extends StatefulWidget {
  final Function(DateTime?) onDateChanged;
  final Function(String?) onTypeChanged;
  final Function(String?) onCategoryChanged;
  final DateTime? initialDate;
  final String? initialType;
  final String? initialCategory;

  const _FilterDialogContent({
    required this.onDateChanged,
    required this.onTypeChanged,
    required this.onCategoryChanged,
    this.initialDate,
    this.initialType,
    this.initialCategory,
  });

  @override
  State<_FilterDialogContent> createState() => _FilterDialogContentState();
}

class _FilterDialogContentState extends State<_FilterDialogContent> {
  late DateTime? selectedDate;
  late String? selectedType;
  late String? selectedCategory;

  @override
  void initState() {
    super.initState();
    selectedDate = widget.initialDate;
    selectedType = widget.initialType;
    selectedCategory = widget.initialCategory;
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: selectedDate ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() {
        selectedDate = picked;
        widget.onDateChanged(picked);
      });
    }
  }

  void _clearFilters() {
    setState(() {
      selectedDate = null;
      selectedType = null;
      selectedCategory = null;
      widget.onDateChanged(null);
      widget.onTypeChanged(null);
      widget.onCategoryChanged(null);
    });
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Filtrer les transmissions'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Filtre par date
            const Text(
              'Date',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 8),
            GestureDetector(
              onTap: _pickDate,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.calendar_today, color: Colors.grey),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        selectedDate == null
                            ? 'Sélectionner une date'
                            : '${selectedDate!.day}/${selectedDate!.month}/${selectedDate!.year}',
                      ),
                    ),
                    if (selectedDate != null)
                      GestureDetector(
                        onTap: () => setState(() {
                          selectedDate = null;
                          widget.onDateChanged(null);
                        }),
                        child: const Icon(Icons.clear, color: Colors.grey),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Filtre par type (interne/externe)
            const Text(
              'Type',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 8),
            DropdownButton<String?>(
              isExpanded: true,
              value: selectedType,
              items: [
                const DropdownMenuItem(
                  value: null,
                  child: Text('Tous les types'),
                ),
                const DropdownMenuItem(
                  value: 'interne',
                  child: Text('Interne'),
                ),
                const DropdownMenuItem(
                  value: 'externe',
                  child: Text('Externe'),
                ),
              ],
              onChanged: (value) {
                setState(() {
                  selectedType = value;
                  widget.onTypeChanged(value);
                });
              },
            ),
            const SizedBox(height: 20),

            // Filtre par catégorie (prêt/déposition)
            const Text(
              'Catégorie',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 8),
            DropdownButton<String?>(
              isExpanded: true,
              value: selectedCategory,
              items: [
                const DropdownMenuItem(
                  value: null,
                  child: Text('Toutes les catégories'),
                ),
                const DropdownMenuItem(
                  value: 'prêt',
                  child: Text('Prêt'),
                ),
                const DropdownMenuItem(
                  value: 'déposition',
                  child: Text('Déposition'),
                ),
              ],
              onChanged: (value) {
                setState(() {
                  selectedCategory = value;
                  widget.onCategoryChanged(value);
                });
              },
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _clearFilters,
          child: const Text('Réinitialiser'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Annuler'),
        ),
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context, {
              'date': selectedDate,
              'type': selectedType,
              'category': selectedCategory,
            });
          },
          child: const Text('Appliquer'),
        ),
      ],
    );
  }
}
