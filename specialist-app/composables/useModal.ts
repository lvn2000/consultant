import { ref, type Ref } from "vue";

export interface ModalOptions {
  closeOnOverlay?: boolean;
  defaultSize?: "sm" | "md" | "lg" | "xl";
}

export interface ModalResult {
  isOpen: Ref<boolean>;
  open: (data?: any) => void;
  close: () => void;
  toggle: () => void;
  modalData: Ref<any>;
}

export function useModal(options: ModalOptions = {}): ModalResult {
  const { closeOnOverlay = true, defaultSize = "md" } = options;
  const isOpen = ref(false);
  const modalData = ref<any>(null);

  const open = (data?: any) => {
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
