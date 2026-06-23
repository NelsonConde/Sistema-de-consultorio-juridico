import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

/**
 * Selector múltiple para agregar y quitar opciones seleccionadas.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.label - Etiqueta principal del selector.
 * @param {string} [props.placeholder] - Texto cuando no hay selección.
 * @param {Array<{value:string,label:string}>} [props.selectedItems] - Items seleccionados.
 * @param {Array<{value:string,label:string}>} [props.availableItems] - Items disponibles.
 * @param {function} props.onSelectionChange - Callback cuando cambia la selección.
 * @param {string} [props.itemLabel] - Etiqueta para la lista de seleccionados.
 * @param {string} [props.addButtonText] - Texto del botón de agregar.
 * @returns {JSX.Element} Componente de selección múltiple.
 */
export function FormMultiSelect({
  label,
  placeholder = "Selecciona una opción para agregar",
  selectedItems = [],
  availableItems = [],
  onSelectionChange,   // TODO: CREEEEOOOO que es mejor revisar la documentación de react
  // sobre el tema de pasar funciones como prop o callbacks, creo que debería ser un hook built-in
  // o un hook personalizado? no recuerdo bien eso, borrar el todo si estaba bien ya
  itemLabel = "opciones seleccionadas",
  addButtonText = "Agregar"
}) {
  const [currentSelection, setCurrentSelection] = useState('');

  // Para no repetir elementos
  const filteredAvailableItems = availableItems.filter(item =>
    !selectedItems.some(selected => selected.value === item.value)
  );

  const addItem = () => {
    if (currentSelection && !selectedItems.some(item => item.value === currentSelection)) {
      const itemToAdd = availableItems.find(item => item.value === currentSelection);
      if (itemToAdd) {
        onSelectionChange([...selectedItems, itemToAdd]);
        setCurrentSelection('');
      }
    }
  };

  const removeItem = (itemValue) => {
    onSelectionChange(selectedItems.filter(item => item.value !== itemValue));
  };


  return (
    <div className="space-y-4">
      <label className="text-sm font-medium">{label}</label>

      {/* Selector de item */}
      <div className="flex gap-2">
        <Select value={currentSelection} onValueChange={setCurrentSelection}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder={placeholder} />
          </SelectTrigger>
          <SelectContent>
            {filteredAvailableItems.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          type="button"
          onClick={addItem}
          disabled={!currentSelection}
        >
          {addButtonText}
        </Button>
      </div>

      {/* Lista con items seleccionados */}
      {selectedItems.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-sm font-medium">{itemLabel}:</h4>
          <div className="flex flex-wrap gap-2">
            {selectedItems.map((item) => (
              <Badge key={item.value} variant="secondary" className="flex items-center gap-1">
                {item.label}
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-4 w-4 p-0 hover:bg-destructive hover:text-destructive-foreground"
                  onClick={() => removeItem(item.value)}
                >
                  x {/* TODO: Buscar algun iconito de Lucid para la X? jsjs */}
                </Button> 
              </Badge>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}