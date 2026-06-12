<?php

namespace App\Http\Controllers\API;

use App\Http\Controllers\Controller;
use App\Models\PersonneAutorisee;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Log;

class PersonneAutoriseeController extends Controller
{
    public function store(Request $request)
    {
        Log::info('Personne autorisée reçue', $request->all());

        $validator = Validator::make($request->all(), [
            'nom' => 'required|string|max:255',
            'poste' => 'required|string|max:255',
            'biomData' => 'required|string',
        ]);

        if ($validator->fails()) {
            Log::error('Validation erreur personne autorisée', $validator->errors()->toArray());
            return response()->json([
                'status' => 'error',
                'errors' => $validator->errors()
            ], 400);
        }

        try {
            $personne = PersonneAutorisee::create($request->only(['nom', 'poste', 'biomData']));

            return response()->json([
                'status' => 'success',
                'message' => 'Personne autorisée enregistrée avec succès',
                'data' => $personne
            ], 201);
        } catch (\Exception $e) {
            Log::error('Erreur création personne autorisée', ['error' => $e->getMessage()]);
            return response()->json([
                'status' => 'error',
                'message' => $e->getMessage()
            ], 500);
        }
    }

    public function biometrics()
    {
        try {
            $biometrics = PersonneAutorisee::select('biomData')
                ->whereNotNull('biomData')
                ->get();

            return response()->json($biometrics, 200);
        } catch (\Exception $e) {
            Log::error('Erreur récupération biométrie', ['error' => $e->getMessage()]);
            return response()->json([
                'status' => 'error',
                'message' => 'Impossible de récupérer les données biométriques'
            ], 500);
        }
    }
}
