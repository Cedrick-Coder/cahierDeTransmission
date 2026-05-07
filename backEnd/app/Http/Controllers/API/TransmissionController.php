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
            'estTerminee'      => 'boolean',
        ]);

        if ($validator->fails()) {
            Log::error('Validation erreur sync', $validator->errors()->toArray());
            return response()->json([
                'status' => 'error',
                'errors' => $validator->errors()
            ], 400);
        }

        try {
            // 2. Création dans la DataBase
            $transmission = Transmission::create($request->all());

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
}