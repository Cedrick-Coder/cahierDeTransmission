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
        'date_remise',
        'estTerminee',
        'is_synced',
    ];

    //'date' doit être traité comme un objet(DateTime)
    
    protected $casts = [
        'date' => 'datetime',
        'date_remise' => 'datetime',
        'estTerminee' => 'boolean',
        'is_synced' => 'boolean',
    ];
}