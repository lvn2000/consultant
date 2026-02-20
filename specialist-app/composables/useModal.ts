import { ref, type Ref } from "vue";

export interface ModalOptions {
  closeOnOverlay?: boolean;
  defaultSize?: "sm" | "md" | "lg" | "xl";
}

export interface ModalResult<T = unknown> {
  isOpen: Ref<boolean>;
  open: (data?: T) => void;
  close: () => void;
  toggle: () => void;
  modalData: Ref<T | null>;
}

export function useModal<T = unknown>(
  _options: ModalOptions = {},
): ModalResult<T> {
  const isOpen = ref(false);
  const modalData = ref<T | null>(null);

  const open = (data?: T) => {
    modalData.value = data;
    isOpen.value = true;
  };

  const close = () => {
    isOpen.value = false;
    modalData.value = null;
  };

  const toggle = () => {
    isOpen.value = !isOpen.value;
  };

  return {
    isOpen,
    open,
    close,
    toggle,
    modalData,
  };
}
