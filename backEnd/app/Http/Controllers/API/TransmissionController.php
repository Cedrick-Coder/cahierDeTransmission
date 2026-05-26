<?php

namespace App\Http\Controllers\API;

use App\Http\Controllers\Controller;
use App\Models\Transmission;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Log;

class TransmissionController extends Controller
{
    public function store(Request $request)
    {
        Log::info('Données reçues', $request->all());
        
        // 1. Validation des données entrantes
        $validator = Validator::make($request->all(), [
            'nom'              => 'required|string|max:255',
            'provenance'       => 'required|string',
            'provenanceResult' => 'nullable|string',
            'type'             => 'required|string',
            'responsable'      => 'nullable|string',
            'quantite'         => 'required|integer',
            'details'          => 'required|string',
            'date'             => 'required|string', // Accepter comme string et laisser Eloquent parser
            'date_remise'      => 'nullable|string',
            'estTerminee'      => 'boolean',
            'is_synced'        => 'boolean',
            'etat'             => 'nullable|string',
            'remarque'         => 'nullable|string|max:75',
        ]);

        if ($validator->fails()) {
            Log::error('Validation erreur sync', $validator->errors()->toArray());
            return response()->json([
                'status' => 'error',
                'errors' => $validator->errors()
            ], 400);
        }

        if ($request->input('type') === 'prêt' && !$request->filled('date_remise')) {
            return response()->json([
                'status' => 'error',
                'errors' => [
                    'date_remise' => ['La date de remise est requise pour un prêt.']
                ]
            ], 400);
        }

        try {
            // 2. Création dans la DataBase
            // Force is_synced à true puisque les données sont maintenant dans le backend
            $data = $request->all();
            $data['is_synced'] = true;
            $transmission = Transmission::create($data);

            // 3. Réponse de succès pour le front
            return response()->json([
                'status'  => 'success',
                'message' => 'Transmission synchronisée avec succès',
                'data'    => $transmission
            ], 201);
        } catch (\Exception $e) {
            Log::error('Erreur sync transmission', ['error' => $e->getMessage()]);
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage()
            ], 500);
        }
    }

    public function index()
    {
        $transmissions = Transmission::all();
        return response()->json($transmissions);
    }

    public function update(Request $request, $id)
    {
        $transmission = Transmission::findOrFail($id);

        $validator = Validator::make($request->all(), [
            'estTerminee' => 'boolean',
            'is_synced' => 'boolean',
            'etat' => 'nullable|string',
            'remarque' => 'nullable|string|max:75',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'status' => 'error',
                'errors' => $validator->errors()
            ], 400);
        }

        $transmission->update($request->only(['estTerminee', 'is_synced', 'etat', 'remarque']));

        return response()->json([
            'status' => 'success',
            'message' => 'Transmission mise à jour',
            'data' => $transmission
        ]);
    }
}