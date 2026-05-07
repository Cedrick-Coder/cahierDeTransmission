<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('transmissions', function (Blueprint $table) {
            $table->id(); // ID auto-incrementé (correspond au int? ID de transmission.dart)
            
            $table->string('nom');
            $table->string('provenance');
            
            // nullable() pour les *optionels
            $table->string('provenanceResult')->nullable(); 
            
            $table->string('type');
            
            $table->string('responsable')->nullable(); 
            
            $table->integer('quantite');
            
            $table->text('details'); 
            
            $table->dateTime('date'); 
            
            $table->boolean('estTerminee')->default(false); 
            
            $table->timestamps(); //permet le suivi de creation et deletion
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('transmissions');
    }
};