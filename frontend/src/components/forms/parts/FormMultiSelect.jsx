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
 * Selection behavior.
 * @param {Object} props - Parameter description.
 * @param {string} props.label - Parameter description.
 * @param {string} [props.placeholder] - Parameter description.
 * Selection behavior.
 * @param {Array<{value:string,label:string}>} [props.availableItems] - Parameter description.
 * @param {function} props.onSelectionChange - Parameter description.
 * @param {string} [props.itemLabel] - Item to process.
 * @param {string} [props.addButtonText] - Parameter description.
 * @returns {JSX.Element} Result value.
 */
export function FormMultiSelect({
  label,
  placeholder = "Selecciona una opción para agregar",
  selectedItems = [],
  availableItems = [],
  onSelectionChange,   // Implementation detail.
  // Implementation detail.
  // Consider extracting this behavior into a custom hook if it becomes reusable.
  itemLabel = "opciones seleccionadas",
  addButtonText = "Agregar"
}) {
  const [currentSelection, setCurrentSelection] = useState('');

  // Implementation detail.
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

      {/* Implementation detail.*/}
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

      {/* List and table handling.*/}
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
                  x {/* Implementation detail.*/}
                </Button> 
              </Badge>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}