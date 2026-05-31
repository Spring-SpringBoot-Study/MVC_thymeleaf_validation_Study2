package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private final ItemRepository itemRepository;

    private final ItemValidator itemValidator;

    @InitBinder
    public void init(WebDataBinder dataBinder) {
        log.info("init binder {}", dataBinder);
        dataBinder.addValidators(itemValidator);
    }

    @GetMapping
    public String items(Model model) {
        List<Item> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "validation/v2/items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/item";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Item()); // 검증이 실패했을 때도, 빈 Item을 넘겨서 이전의 model에 작성된 다른 속성 입력값들을 가져올 수 있음
        return "validation/v2/addForm";
    }

    // @PostMapping("/add")
    public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        // BindingResult는 ModelAttribute인 Item 객체의 바인딩 결과를 담고 있기 때문에, 무조건 @ModelAttribute Item item의 뒤에 와야함

        // 검증 오류 결과를 보관
        // Map<String, String> errors = new HashMap<>();
        // 이제 BlindinfResult가 기존 errors의 역할을 해줌

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            // errors.put("itemName", "상품 이름은 필수입니다.");
            bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            // errors.put("price", "가격은 1,000 ~ 1,000,000 까지 허용합니다.");
            bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }

        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            // errors.put("quantity", "수량은 최대 9,999 까지 허용합니다.");
            bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // errors.put("globalError", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice);
                // new ObjectError()로 만들면, GlobalErrors로 등록됨
                bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로
//        if (!errors.isEmpty()) {
//            log.info("errors: {}", errors);
//            model.addAttribute("errors", errors);
//            return "validation/v2/addForm";
//        }
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            // bindingResult는 자동으로 view에 같이 넘어가기 때문에, model.addAttribute에 안 담아도 됨
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    // @PostMapping("/add")
    public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        // BindingResult는 ModelAttribute인 Item 객체의 바인딩 결과를 담고 있기 때문에, 무조건 @ModelAttribute Item item의 뒤에 와야함

        // 검증 오류 결과를 보관
        // Map<String, String> errors = new HashMap<>();
        // 이제 BlindinfResult가 기존 errors의 역할을 해줌

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            // errors.put("itemName", "상품 이름은 필수입니다.");
            // bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
            // 사용자가 입력한 값을 3번째 파라미터인 item.getItemName()처럼 보존해서, 잘못 입력했을 시에도 사라지지 않고 보존되게 함(필드가 맞지 않은 입력값도 보존함)
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, null, null, "상품 이름은 필수입니다."));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            // errors.put("price", "가격은 1,000 ~ 1,000,000 까지 허용합니다.");
            // bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null, "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }

        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            // errors.put("quantity", "수량은 최대 9,999 까지 허용합니다.");
            // bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null, "수량은 최대 9,999 까지 허용합니다."));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // errors.put("globalError", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice);
                // new ObjectError()로 만들면, GlobalErrors로 등록됨
                bindingResult.addError(new ObjectError("item", null, null, "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로
//        if (!errors.isEmpty()) {
//            log.info("errors: {}", errors);
//            model.addAttribute("errors", errors);
//            return "validation/v2/addForm";
//        }
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            // bindingResult는 자동으로 view에 같이 넘어가기 때문에, model.addAttribute에 안 담아도 됨
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    // @PostMapping("/add")
    public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        // BindingResult는 ModelAttribute인 Item 객체의 바인딩 결과를 담고 있기 때문에, 무조건 @ModelAttribute Item item의 뒤에 와야함

        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());

        // 검증 오류 결과를 보관
        // Map<String, String> errors = new HashMap<>();
        // 이제 BlindinfResult가 기존 errors의 역할을 해줌

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            // errors.put("itemName", "상품 이름은 필수입니다.");
            // bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
            // 사용자가 입력한 값을 3번째 파라미터인 item.getItemName()처럼 보존해서, 잘못 입력했을 시에도 사라지지 않고 보존되게 함(필드가 맞지 않은 입력값도 보존함)
            // 5번쨰 파라미터인 code에서 error.properties 파일의 코드를 불러옴. 이게 없으면 default 사용
            // new String 배열로 사용하는 이유는, 첫번째 인덱스의 값이 없으면(선언이 안되어 있으면), 다음 인덱스의 값을 사용
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, new String[]{"required.item.itemName"}, null, "default 오류 메시지"));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            // errors.put("price", "가격은 1,000 ~ 1,000,000 까지 허용합니다.");
            // bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000, 1000000}, "default 오류 메시지"));
        }

        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            // errors.put("quantity", "수량은 최대 9,999 까지 허용합니다.");
            // bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, "default 오류 메시지"));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // errors.put("globalError", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice);
                // new ObjectError()로 만들면, GlobalErrors로 등록됨
                bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000, resultPrice}, "default 오류 메시지"));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로
//        if (!errors.isEmpty()) {
//            log.info("errors: {}", errors);
//            model.addAttribute("errors", errors);
//            return "validation/v2/addForm";
//        }
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            // bindingResult는 자동으로 view에 같이 넘어가기 때문에, model.addAttribute에 안 담아도 됨
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    // @PostMapping("/add")
    public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        // BindingResult는 ModelAttribute인 Item 객체의 바인딩 결과를 담고 있기 때문에, 무조건 @ModelAttribute Item item의 뒤에 와야함

        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());

        // 검증 오류 결과를 보관
        // Map<String, String> errors = new HashMap<>();
        // 이제 BlindinfResult가 기존 errors의 역할을 해줌

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            // bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, new String[]{"required.item.itemName"}, null, "default 오류 메시지"));
            // 위의 코드와 동일한 기능
            bindingResult.rejectValue("itemName", "required");
        }

        // ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required"); // 위의 코드와 동일한 기능

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            // bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000, 1000000}, "default 오류 메시지"));
            bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
        }

        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            // bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, "default 오류 메시지"));
            bindingResult.rejectValue("quantity", "max", new Object[]{9999}, null);
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000, resultPrice}, "default 오류 메시지"));
                bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
            }
        }

        // 검증에 실패하면 다시 입력 폼으로
//        if (!errors.isEmpty()) {
//            log.info("errors: {}", errors);
//            model.addAttribute("errors", errors);
//            return "validation/v2/addForm";
//        }
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            // bindingResult는 자동으로 view에 같이 넘어가기 때문에, model.addAttribute에 안 담아도 됨
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    // @PostMapping("/add")
    public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        itemValidator.validate(item, bindingResult); // 검증 로직들을 모두 itemValidator가 처리함

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        //성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);

        return "redirect:/validation/v2/items/{itemId}";
    }

    @PostMapping("/add")
    // @Validated 애노태이션 추가 시, @InitBinder로 해당 컨트롤러에서 설정한 validator로 Item 객체를 검증할 수 있음.
    // 검증기를 실행하라! 라는 애노태이션임
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";

        }

        //성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);

        return "redirect:/validation/v2/items/{itemId}";
    }

    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute Item item) {
        itemRepository.update(itemId, item);
        return "redirect:/validation/v2/items/{itemId}";
    }

}

