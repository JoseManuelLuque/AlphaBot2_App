package com.jluqgon214.alphabot2.navigation

sealed class Screen(val route: String) {
    object Config : Screen("config")
    object Main : Screen("main/{host}/{user}/{password}") {
        fun createRoute(host: String, user: String, password: String): String {
            return "main/$host/$user/$password"
        }
    }
}

