# BagTracker

Application Android de gestion d'inventaire personnel — rangements, sacs, boîtes, valises — avec hiérarchie de contenants, alertes de péremption, rappels personnalisés, QR codes et statistiques.

---

## Sommaire

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Dépendances](#dépendances)
- [Structure du projet](#structure-du-projet)
- [Base de données](#base-de-données)
- [Navigation](#navigation)
- [Widget](#widget)
- [Export / Import](#export--import)
- [Notifications](#notifications)
- [Exemples d'utilisation](#exemples-dutilisation)

---

## Aperçu

BagTracker permet de savoir en permanence où se trouve chaque objet, dans quel contenant il est rangé, quand il périme, et combien il vaut. L'inventaire est hiérarchique : un sac peut contenir une trousse, qui contient des médicaments, qui ont une date de péremption.

L'application fonctionne entièrement hors-ligne, sans compte, sans serveur. Toutes les données sont stockées localement via Room.

---

## Fonctionnalités

### Inventaire hiérarchique
- Création d'objets et de contenants (sacs, boîtes, tiroirs...)
- Imbrication illimitée : contenant dans un contenant
- Navigation drill-down dans l'arborescence
- Breadcrumb affiché sur chaque fiche objet (ex: `Chambre › Armoire › Trousse › Doliprane`)

### Fiche objet
Chaque objet possède :
- Nom, emoji ou photo
- Quantité + unité (ml, g, comprimés, unités...)
- État : neuf / bon / usé / à remplacer / cassé
- Date de péremption avec alerte colorée (vert → rouge)
- Poids (g/kg), volume (cm³), prix estimé (€)
- Code-barre (scan ou saisie manuelle)
- Localisation GPS optionnelle (ouvre Google Maps au tap)
- Tags libres (ex: `#médecine`, `#camping`, `#cuisine`)
- Rappels périodiques personnalisés
- Notes libres

### Recherche
- Recherche full-text sur le nom, les notes, les tags et le code-barre
- Résultats en temps réel au fil de la frappe
- Accès depuis la bottom navigation ou le widget

### Mode guidé
- Sélectionne un objet et lance le mode guidé
- L'app affiche le chemin pas à pas pour retrouver l'objet physiquement
- Ex: `Ouvre : Chambre` → `Ouvre : Armoire` → `Ouvre : Trousse` → `C'est là : Doliprane`

### Liste plate et tri
- Vue à plat de tous les objets (hors contenants)
- Tri par : nom, date de péremption, valeur, poids, dernière vue

### QR Codes
- Génération d'un QR code pour chaque objet
- Le QR encode un deep link `bagtracker://item/{id}/{nom}`
- Scan du QR ouvre directement la fiche dans l'app
- Sauvegarde du QR en PNG local

### Statistiques
- Nombre total d'objets et de contenants
- Valeur estimée totale de l'inventaire
- Compteurs : périmés, périment bientôt, à remplacer, pas vus depuis 6 mois
- Graphique camembert de répartition par tag (MPAndroidChart)

### Historique des mouvements
- Chaque déplacement d'objet est loggué automatiquement
- Affichage chronologique sur la fiche objet et dans l'onglet historique
- Actions tracées : `ADD`, `MOVE`, `DELETE`

### Rappels personnalisés
- Associe des rappels à n'importe quel objet
- Intervalle configurable en jours (ex: "Recharger la batterie tous les 30 jours")
- Notification push à l'échéance, avec bouton "Fait" pour relancer le compteur

### Export / Import
- Export JSON de l'inventaire complet (items + tags)
- Partage via le sélecteur système Android
- Import JSON avec fusion dans la base existante
- Nettoyage automatique des 5 exports les plus anciens

### Widget home screen
- Affiche en temps réel le nombre d'objets périmés et périment bientôt
- Tap sur le widget → ouvre l'app
- Bouton raccourci → ouvre directement la recherche
- Se rafraîchit automatiquement après chaque modification

### Requête récursive (chemin complet)

Room supporte les CTE récursives SQLite. Le chemin d'un objet est calculé ainsi :

```sql
WITH RECURSIVE path(id, name, parentId, depth) AS (
    SELECT id, name, parentId, 0 FROM items WHERE id = :itemId
    UNION ALL
    SELECT i.id, i.name, i.parentId, p.depth + 1
    FROM items i INNER JOIN path p ON i.id = p.parentId
)
SELECT * FROM path ORDER BY depth DESC
```

Résultat pour `Doliprane` rangé dans `Trousse › Armoire › Chambre` :
```
depth 2 → Chambre
depth 1 → Armoire
depth 0 → Trousse
depth -1 → Doliprane   (l'objet lui-même)
```

### Deep link

Un QR code scanné depuis une autre app déclenche :
```
bagtracker://item/{itemId}/{itemName}
```
`MainActivity` intercepte cet intent et navigue directement vers `itemDetailFragment`.

---

## Widget

Le widget home screen affiche :
- Nombre d'objets périmés
- Nombre d'objets périment dans les 7 prochains jours

Il se rafraîchit :
- Automatiquement toutes les heures (via `updatePeriodMillis`)
- Immédiatement après chaque `upsertItem`, `updateItem`, `deleteItem` ou `importFromUri` via `BagTrackerWidgetProvider.refresh(context)`

---

## Export / Import

### Format JSON

```json
{
  "exportedAt": "Mon Jan 01 12:00:00 CET 2025",
  "version": 2,
  "items": [
    {
      "item": {
        "id": 1,
        "name": "Doliprane 500mg",
        "isContainer": false,
        "quantity": 12,
        "quantityUnit": "comprimés",
        "condition": "bon",
        "expirationDate": 1735689600000,
        "parentId": 3,
        "emoji": "💊",
        "priceCents": 350
      },
      "tags": [
        { "id": 2, "name": "médecine", "color": "#6C5CE8" }
      ]
    }
  ]
}
```

### Import
L'import fusionne les données : les tags existants sont réutilisés par nom, les items sont réinsérés avec de nouveaux IDs pour éviter les collisions.

---

## Notifications

Deux canaux Android distincts :

| Canal | Déclencheur | Fréquence |
|---|---|---|
| `channel_expiry` | Objets périmant dans ≤ 3 jours | 1x par jour (WorkManager) |
| `channel_reminder` | Rappels dont `nextTriggerAt ≤ now` | 1x toutes les 6h (WorkManager) |

Les workers sont enregistrés au démarrage de `MainActivity` avec `ExistingPeriodicWorkPolicy.KEEP` pour éviter les doublons.

---


## Prérequis

- Android 8.0 minimum (API 26)
- Kotlin 1.9+
- Android Studio Hedgehog ou supérieur
- Pas de connexion réseau requise

---
