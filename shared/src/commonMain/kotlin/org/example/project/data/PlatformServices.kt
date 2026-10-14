package org.example.project.data

import com.russhwolf.settings.Settings
import org.example.project.pet.PetState

expect fun createSettings(): Settings

expect fun createStepDataSource(): StepDataSource
