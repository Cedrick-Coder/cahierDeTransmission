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
        Schema::table('transmissions', function (Blueprint $table) {
            $table->string('etat')->default('suivi')->after('estTerminee');
            $table->string('remarque', 75)->nullable()->after('etat');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('transmissions', function (Blueprint $table) {
            if (Schema::hasColumn('transmissions', 'remarque')) {
                $table->dropColumn('remarque');
            }
            if (Schema::hasColumn('transmissions', 'etat')) {
                $table->dropColumn('etat');
            }
        });
    }
};
