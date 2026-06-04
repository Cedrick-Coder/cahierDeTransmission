<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PersonneAutorisee extends Model
{
    protected $table = 'personneOtorisé';

    protected $fillable = [
        'nom',
        'poste',
        'biomData',
    ];

    protected $casts = [
        'nom' => 'string',
        'poste' => 'string',
        'biomData' => 'string',
    ];
}
