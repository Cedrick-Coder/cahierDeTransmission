import 'package:flutter/material.dart';

class ProvenanceSelector extends StatelessWidget {
  final String provenance;
  final ValueChanged<String?> onChanged;

  const ProvenanceSelector({super.key, required this.provenance, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return RadioGroup<String>(
      groupValue: provenance,
      onChanged: onChanged,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "Provenance :",
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Row(
            children: [
              const Text("Interne"),
              Radio<String>(value: "interne"),
              const SizedBox(width: 12),
              const Text("Externe"),
              Radio<String>(value: "externe"),
            ],
          ),
        ],
      ),
    );
  }
}

class DirectionDropdown extends StatelessWidget {
  final List<String> directions;
  final String? selectedDirection;
  final ValueChanged<String?> onChanged;
  final String? Function(String?)? validator;

  const DirectionDropdown({
    super.key,
    required this.directions,
    required this.selectedDirection,
    required this.onChanged,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String>(
      initialValue: selectedDirection,
      decoration: const InputDecoration(
        labelText: "Sélectionner la Direction",
        border: OutlineInputBorder(),
      ),
      items: directions
          .map((dir) => DropdownMenuItem(value: dir, child: Text(dir)))
          .toList(),
      onChanged: onChanged,
      validator: validator,
    );
  }
}

class AgenceCaisseSelector extends StatelessWidget {
  final List<String> agences;
  final String? selectedAgence;
  final String? selectedCaisse;
  final List<String> caissesDisponibles;
  final ValueChanged<String?> onAgenceChanged;
  final ValueChanged<String?> onCaisseChanged;
  final String? Function(String?)? agenceValidator;
  final String? Function(String?)? caisseValidator;

  const AgenceCaisseSelector({
    super.key,
    required this.agences,
    required this.selectedAgence,
    required this.selectedCaisse,
    required this.caissesDisponibles,
    required this.onAgenceChanged,
    required this.onCaisseChanged,
    this.agenceValidator,
    this.caisseValidator,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: DropdownButtonFormField<String>(
            initialValue: selectedAgence,
            decoration: const InputDecoration(
              labelText: "Agence",
              border: OutlineInputBorder(),
            ),
            items: agences
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
            onChanged: onAgenceChanged,
            validator: agenceValidator,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: DropdownButtonFormField<String>(
            initialValue: selectedCaisse,
            decoration: const InputDecoration(
              labelText: "Caisse",
              border: OutlineInputBorder(),
            ),
            items: caissesDisponibles
                .map(
                  (caisse) => DropdownMenuItem(
                    value: caisse,
                    child: Text(
                      caisse,
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                )
                .toList(),
            onChanged: onCaisseChanged,
            validator: caisseValidator,
          ),
        ),
      ],
    );
  }
}

class TypeSelector extends StatelessWidget {
  final String type;
  final ValueChanged<String?> onChanged;

  const TypeSelector({super.key, required this.type, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return RadioGroup<String>(
      groupValue: type,
      onChanged: onChanged,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "Type de mouvement :",
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Row(
            children: [
              const Text("Prêt"),
              Radio<String>(value: "prêt"),
              const SizedBox(width: 12),
              const Text("Déposition"),
              Radio<String>(value: "déposition"),
            ],
          ),
        ],
      ),
    );
  }
}

class DateRemiseField extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onPickDate;
  final VoidCallback onClearDate;
  final String? Function(String?)? validator;

  const DateRemiseField({
    super.key,
    required this.controller,
    required this.onPickDate,
    required this.onClearDate,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      readOnly: true,
      validator: validator,
      decoration: InputDecoration(
        labelText: "Date de remise",
        border: OutlineInputBorder(),
        floatingLabelBehavior: FloatingLabelBehavior.always,
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4.0),
          borderSide: BorderSide(color: Colors.grey.shade400),
        ),
        suffixIcon: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(
                Icons.calendar_month_outlined,
                color: Colors.black87,
              ),
              onPressed: onPickDate,
            ),
            if (controller.text != "00-00-0000")
              IconButton(
                icon: const Icon(
                  Icons.close,
                  color: Colors.grey,
                  size: 20,
                ),
                onPressed: onClearDate,
              ),
          ],
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 15,
        ),
      ),
      style: const TextStyle(fontSize: 16, letterSpacing: 1.2),
    );
  }
}
