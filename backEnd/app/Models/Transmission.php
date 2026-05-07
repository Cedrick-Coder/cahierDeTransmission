<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Transmission extends Model
{
    // On définit les champs autorisés via l'API
    protected $fillable = [
        'nom',
        'provenance',
        'provenanceResult',
        'type',
        'responsable',
        'quantite',
        'details',
        'date',
        'estTerminee',
    ];

    //'date' doit être traité comme un objet(DateTime)
    
    protected $casts = [
        'date' => 'datetime',
        'estTerminee' => 'boolean',
    ];
}